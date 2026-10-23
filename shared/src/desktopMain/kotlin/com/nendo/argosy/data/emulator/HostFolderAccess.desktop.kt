package com.nendo.argosy.data.emulator

import com.nendo.argosy.data.model.EmulatorFolderAccess
import com.nendo.argosy.platform.HostShell
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
actual class HostFolderAccess @Inject constructor(
    private val emulatorDetector: EmulatorDetector
) {
    actual val isSupported: Boolean = true

    actual suspend fun emulators(folder: String): List<EmulatorFolderAccess> = withContext(Dispatchers.IO) {
        if (emulatorDetector.installedEmulators.value.isEmpty()) emulatorDetector.detectEmulators()
        val home = System.getProperty("user.home")
        LinuxEmulators.all
            .mapNotNull { linux ->
                val def = EmulatorRegistry.getById(linux.id) ?: return@mapNotNull null
                val install = emulatorDetector.linuxInstall(def.packageName)
                    ?.takeIf { it.kind == LinuxInstallKind.FLATPAK } ?: return@mapNotNull null
                install.target to linux.displayName
            }
            .distinctBy { it.first }
            .map { (appId, name) ->
                EmulatorFolderAccess(appId, name, FlatpakFilesystems.covers(permissions(appId), folder, home))
            }
    }

    actual suspend fun grant(appId: String, folder: String): Boolean = withContext(Dispatchers.IO) {
        HostShell.run(listOf("mkdir", "-p", folder))
        HostShell.run(listOf("flatpak", "override", "--user", "--filesystem=$folder", appId))
        FlatpakFilesystems.covers(permissions(appId), folder, System.getProperty("user.home"))
    }

    private fun permissions(appId: String): List<String> =
        FlatpakFilesystems.parse(HostShell.run(listOf("flatpak", "info", "--show-permissions", appId)).orEmpty())
}

/**
 * Reads the `filesystems=` line of `flatpak info --show-permissions`, which already merges the
 * app's own manifest with user and system overrides.
 */
object FlatpakFilesystems {
    private val WHOLE_HOME = setOf("home", "~")

    fun parse(output: String): List<String> =
        output.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("filesystems=") }
            ?.removePrefix("filesystems=")
            ?.split(';')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

    fun covers(entries: List<String>, folder: String, home: String): Boolean {
        val target = folder.trimEnd('/')
        val (denied, allowed) = entries.map { it.substringBefore(':') }.partition { it.startsWith("!") }
        fun matches(entry: String): Boolean {
            if (entry == "host") return true
            if (entry in WHOLE_HOME) return target.startsWith("$home/")
            val path = when {
                entry.startsWith("~/") -> "$home/${entry.removePrefix("~/")}"
                entry.startsWith("/") -> entry
                else -> return false
            }.trimEnd('/')
            return target == path || target.startsWith("$path/")
        }
        return allowed.any(::matches) && denied.none { matches(it.removePrefix("!")) }
    }
}
