package com.nendo.argosy.platform

import com.nendo.argosy.data.emulator.FlatpakFilesystems
import com.nendo.argosy.data.model.SandboxPermission
import com.nendo.argosy.data.model.SandboxPermissionReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class SandboxPermissions @Inject constructor() {
    actual val isSupported: Boolean = true

    actual suspend fun read(): SandboxPermissionReport = withContext(Dispatchers.IO) {
        val info = runCatching { File(FLATPAK_INFO).readText() }.getOrNull()
        if (info == null) {
            SandboxPermissionReport(sandboxed = false, granted = SandboxPermission.entries.associateWith { true })
        } else {
            FlatpakInfo.report(info, System.getProperty("user.home"))
        }
    }

    actual suspend fun grant(permission: SandboxPermission, flags: List<String>): Boolean = withContext(Dispatchers.IO) {
        HostShell.run(SandboxPermission.overrideArgs(flags))
        val hostView = HostShell.run(listOf("flatpak", "info", "--show-permissions", SandboxPermission.APP_ID))
            ?: return@withContext false
        FlatpakInfo.report(hostView, System.getProperty("user.home")).granted[permission] == true
    }

    private companion object {
        const val FLATPAK_INFO = "/.flatpak-info"
    }
}

/**
 * Reads `/.flatpak-info`, the keyfile Flatpak writes into every sandbox with the permissions in
 * force for this run: the manifest merged with user and system overrides.
 */
object FlatpakInfo {
    val EMULATOR_FOLDERS = listOf(
        "~/Emulation",
        "~/.var/app/org.libretro.RetroArch",
        "~/.var/app/org.DolphinEmu.dolphin-emu",
        "~/.var/app/info.cemu.Cemu",
        "~/.var/app/org.azahar_emu.Azahar",
        "~/.var/app/io.github.ryubing.Ryujinx",
        "~/.var/app/dev.eden_emu.eden",
        "~/.var/app/org.duckstation.DuckStation",
        "~/.var/app/net.pcsx2.PCSX2",
        "~/.var/app/org.ppsspp.PPSSPP",
        "~/.var/app/net.rpcs3.RPCS3",
        "xdg-config/retroarch",
        "xdg-config/Ryujinx",
        "xdg-config/PCSX2",
        "xdg-config/ppsspp",
        "xdg-config/rpcs3",
        "xdg-data/Cemu",
        "xdg-data/azahar-emu",
        "xdg-data/eden",
        "xdg-data/duckstation",
        "xdg-data/Vita3K",
        "xdg-data/dolphin-emu",
        "~/.dolphin-emu",
    )

    fun report(info: String, home: String): SandboxPermissionReport {
        val sections = parse(info)
        val context = sections[CONTEXT].orEmpty()
        fun list(key: String) = context[key]?.split(';')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
        val filesystems = list("filesystems")
        val shared = list("shared")
        val sockets = list("sockets")
        val devices = list("devices")
        val sessionBus = sections[SESSION_BUS].orEmpty()
        val missingFolders = EMULATOR_FOLDERS.filterNot { hasFilesystem(filesystems, it, home) }
        val granted = SandboxPermission.entries.associateWith { permission ->
            when (permission) {
                SandboxPermission.GAMES_FOLDER -> hasFilesystem(filesystems, "~/Games/Argosy", home)
                SandboxPermission.EMULATOR_FOLDERS -> missingFolders.isEmpty()
                SandboxPermission.HOST_ACCESS -> sessionBus["org.freedesktop.Flatpak"] == "talk"
                SandboxPermission.NOTIFICATIONS, SandboxPermission.SCREENSHOTS -> true
                SandboxPermission.CONTROLLERS -> "all" in devices || "input" in devices
                SandboxPermission.NETWORK -> "network" in shared
                SandboxPermission.AUDIO -> "pulseaudio" in sockets
                SandboxPermission.GPU -> "dri" in devices || "all" in devices
                SandboxPermission.DISPLAY -> "wayland" in sockets || "x11" in sockets || "fallback-x11" in sockets
            }
        }
        val missingFlags = granted.filterValues { !it }.keys.associateWith { permission ->
            if (permission == SandboxPermission.EMULATOR_FOLDERS) {
                missingFolders.map { "--filesystem=$it" }
            } else {
                listOf(permission.flag)
            }
        }
        return SandboxPermissionReport(sandboxed = true, granted = granted, missingFlags = missingFlags)
    }

    private fun hasFilesystem(entries: List<String>, wanted: String, home: String): Boolean {
        val plain = entries.map { it.substringBefore(':') }
        if (wanted in plain) return true
        if (!wanted.startsWith("~/")) return false
        return FlatpakFilesystems.covers(entries, "$home/${wanted.removePrefix("~/")}", home)
    }

    private fun parse(info: String): Map<String, Map<String, String>> {
        val sections = linkedMapOf<String, MutableMap<String, String>>()
        var current: MutableMap<String, String>? = null
        info.lineSequence().map { it.trim() }.forEach { line ->
            when {
                line.startsWith("[") && line.endsWith("]") ->
                    current = sections.getOrPut(line.substring(1, line.length - 1)) { linkedMapOf() }
                '=' in line && !line.startsWith("#") ->
                    current?.put(line.substringBefore('=').trim(), line.substringAfter('=').trim())
            }
        }
        return sections
    }

    private const val CONTEXT = "Context"
    private const val SESSION_BUS = "Session Bus Policy"
}
