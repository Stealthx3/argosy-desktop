package com.nendo.argosy.data.emulator

import com.nendo.argosy.platform.DesktopDirectories
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * The Linux emulator compatibility database: which emulators Argosy detects, how to find each
 * install (Flatpak ids, binaries, AppImage names) and how to launch a game in it. The copy bundled
 * with the app is the floor; [refresh] caches a newer copy from the project repository, and the
 * higher `version` wins. Entry ids are upstream emulator ids from `EmulatorRegistry`.
 */
object LinuxEmulatorDatabase {
    data class Database(val version: Int, val emulators: List<LinuxEmulator>)

    const val REMOTE_URL =
        "https://raw.githubusercontent.com/Stealthx3/argosy-desktop/main/shared/src/desktopMain/resources/emulators/linux-emulators.json"
    private const val RESOURCE = "/emulators/linux-emulators.json"
    private const val CACHE_NAME = "linux-emulators.json"
    private const val TIMEOUT_SECONDS = 15L

    @Volatile
    private var loaded: Database? = null

    val current: Database
        get() = loaded ?: load().also { loaded = it }

    fun bundled(): Database = parse(requireNotNull(javaClass.getResource(RESOURCE)).readText())

    fun parse(json: String): Database {
        val root = JSONObject(json)
        val emulators = root.getJSONArray("emulators").objects().map { entry ->
            LinuxEmulator(
                id = entry.getString("id"),
                displayName = entry.getString("name"),
                flatpakIds = entry.strings("flatpakIds"),
                binaries = entry.strings("binaries"),
                appImagePrefixes = entry.strings("appImagePrefixes"),
                installPaths = entry.strings("installPaths"),
                launch = entry.optJSONObject("launch")?.let { launch ->
                    LinuxLaunchArgs(
                        args = launch.strings("args"),
                        onlyTargetsContaining = launch.optString("onlyTargetsContaining").takeIf { it.isNotEmpty() },
                    )
                },
            )
        }
        require(emulators.isNotEmpty()) { "empty emulator database" }
        return Database(version = root.getInt("version"), emulators = emulators)
    }

    fun refresh(fetch: (String) -> String? = ::httpGet, cacheFile: File = defaultCacheFile()): Boolean {
        val text = runCatching { fetch(REMOTE_URL) }.getOrNull() ?: return false
        val remote = runCatching { parse(text) }.getOrNull() ?: return false
        if (remote.version <= current.version) return false
        cacheFile.parentFile?.mkdirs()
        val temp = File(cacheFile.parentFile, cacheFile.name + ".tmp")
        temp.writeText(text)
        if (!temp.renameTo(cacheFile)) return false
        loaded = remote
        return true
    }

    fun load(cacheFile: File = defaultCacheFile()): Database {
        val bundled = bundled()
        val cached = runCatching { parse(cacheFile.readText()) }.getOrNull()
        return if (cached != null && cached.version > bundled.version) cached else bundled
    }

    private fun defaultCacheFile(): File = DesktopDirectories.data.resolve("emulators").resolve(CACHE_NAME)

    private fun httpGet(url: String): String? {
        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS)).build()
        val request = HttpRequest.newBuilder(URI(url)).timeout(Duration.ofSeconds(TIMEOUT_SECONDS)).GET().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body().takeIf { response.statusCode() == 200 }
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun JSONObject.strings(key: String): List<String> =
        optJSONArray(key)?.let { array -> (0 until array.length()).map { array.getString(it) } }.orEmpty()
}
