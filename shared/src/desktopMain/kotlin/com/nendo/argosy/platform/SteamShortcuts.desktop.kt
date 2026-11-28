package com.nendo.argosy.platform

import com.nendo.argosy.data.model.SandboxPermission
import com.nendo.argosy.data.model.SteamShortcutAvailability
import com.nendo.argosy.data.model.SteamShortcutOperation
import com.nendo.argosy.data.model.SteamShortcutOutcome
import com.nendo.argosy.data.model.SteamShortcutStatus
import com.nendo.argosy.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

actual class SteamShortcuts @Inject constructor() {
    private val helper = File("/app/bin/${SandboxPermission.APP_ID}-steam")
    private val config = File("/app/share/${SandboxPermission.APP_ID}/steam/steam-shortcut.json")

    actual val isSupported: Boolean get() = helper.canExecute() && config.isFile

    actual suspend fun status(): SteamShortcutStatus = withContext(Dispatchers.IO) {
        if (!isSupported) {
            SteamShortcutStatus()
        } else {
            val status = SteamHelperOutput.status(runHelper("status"))
            if (status.present && BrandRelocation.steamShortcutStaleFlag().exists()) status.copy(present = false) else status
        }
    }

    actual suspend fun run(operation: SteamShortcutOperation): SteamShortcutOutcome = withContext(Dispatchers.IO) {
        if (!isSupported) return@withContext SteamShortcutOutcome(operation, success = false)
        val result = when (operation) {
            SteamShortcutOperation.ADD -> runHelper("add", "--yes")
            SteamShortcutOperation.UPDATE_ART -> runHelper("refresh")
            SteamShortcutOperation.REMOVE -> runHelper("remove", "--yes")
        }
        SteamHelperOutput.outcome(operation, result).also { outcome ->
            if (operation == SteamShortcutOperation.ADD && outcome.success) BrandRelocation.steamShortcutStaleFlag().delete()
        }
    }

    fun refreshArtInBackground() {
        if (!isSupported) return
        Thread({ runCatching { runHelper("refresh") } }, "steam-art-refresh").apply { isDaemon = true }.start()
    }

    private fun runHelper(vararg args: String): HelperResult = try {
        val process = ProcessBuilder(listOf(helper.path) + args.toList() + listOf("--config", config.path))
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            HelperResult(process.exitValue(), output)
        } else {
            process.destroyForcibly()
            HelperResult(-1, "")
        }
    } catch (e: Exception) {
        Logger.warn(TAG, "Steam helper failed: ${e.message}")
        HelperResult(-1, "")
    }

    private companion object {
        const val TAG = "SteamShortcuts"
        const val TIMEOUT_SECONDS = 60L
    }
}

internal data class HelperResult(val exitCode: Int, val output: String)

internal object SteamHelperOutput {
    fun status(result: HelperResult): SteamShortcutStatus {
        val json = parse(result.output) ?: return SteamShortcutStatus(SteamShortcutAvailability.NOT_FOUND)
        if (json.optBoolean("steam", true) == false) return SteamShortcutStatus(SteamShortcutAvailability.NOT_FOUND)
        val results = json.optJSONArray("results") ?: return SteamShortcutStatus(SteamShortcutAvailability.NOT_FOUND)
        val entries = (0 until results.length()).mapNotNull { results.optJSONObject(it) }
        return SteamShortcutStatus(
            availability = SteamShortcutAvailability.FOUND,
            present = entries.any { it.optBoolean("present") },
            flatpakSteam = entries.any { it.optBoolean("flatpak_steam") },
            accounts = entries.size,
        )
    }

    fun outcome(operation: SteamShortcutOperation, result: HelperResult): SteamShortcutOutcome {
        val results = parse(result.output)?.optJSONArray("results")
        val changed = results?.let { r -> (0 until r.length()).count { r.optJSONObject(it)?.optBoolean("changed") == true } } ?: 0
        return SteamShortcutOutcome(operation, success = result.exitCode == 0 && changed > 0, accounts = changed)
    }

    private fun parse(output: String): JSONObject? = runCatching { JSONObject(output.trim()) }.getOrNull()
}
