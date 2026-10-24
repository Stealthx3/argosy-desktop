package com.nendo.argosy.data.emulator

import com.nendo.argosy.platform.PlatformIntentType as Intent

/**
 * Host command lines for a Linux install. Every argument form is taken from the emulator's own
 * `--help`: RetroArch `-L <core> <content>`, Dolphin `-b -e <file>`, PPSSPP `[FILE]`,
 * Cemu `-g <path>`, DOSBox-X `[name]`. An emulator without a verified form has no command.
 */
object LinuxLaunch {
    const val EXTRA_HOST_COMMAND = "argosy.desktop.host_command"
    const val EXTRA_HOST_DIRECTORY = "argosy.desktop.host_directory"

    private const val RETROARCH_FLATPAK = "org.libretro.RetroArch"

    fun prefix(install: LinuxInstall): List<String> = when (install.kind) {
        LinuxInstallKind.FLATPAK -> listOf("flatpak", "run", install.target)
        LinuxInstallKind.APPIMAGE, LinuxInstallKind.NATIVE -> listOf(install.target)
    }

    fun retroArch(install: LinuxInstall, corePath: String, rom: String): List<String> =
        prefix(install) + listOf("-L", corePath, rom)

    fun standalone(emulatorId: String, install: LinuxInstall, rom: String): List<String>? {
        val launch = LinuxEmulators.byId(emulatorId)?.launch ?: return null
        val only = launch.onlyTargetsContaining
        if (only != null && !install.target.contains(only, ignoreCase = true)) return null
        return prefix(install) + launch.args.map { it.replace(ROM_PLACEHOLDER, rom) }
    }

    private const val ROM_PLACEHOLDER = "{rom}"

    fun retroArchConfigPath(install: LinuxInstall, home: String): String =
        if (install.kind == LinuxInstallKind.FLATPAK && install.target == RETROARCH_FLATPAK) {
            "$home/.var/app/$RETROARCH_FLATPAK/config/retroarch/retroarch.cfg"
        } else {
            "$home/.config/retroarch/retroarch.cfg"
        }

    /**
     * Where RetroArch looks for [coreName]: the config's `libretro_directory` first, then the
     * install's default core folder, then the distribution library folders for a native install.
     */
    fun retroArchCoreCandidates(install: LinuxInstall, home: String, coreName: String, config: String?): List<String> {
        val file = "${coreName}_libretro.so"
        val configured = config?.let { parseLibretroDirectory(it, home) }
        val defaults = if (install.kind == LinuxInstallKind.FLATPAK && install.target == RETROARCH_FLATPAK) {
            listOf("$home/.var/app/$RETROARCH_FLATPAK/config/retroarch/cores")
        } else {
            listOf(
                "$home/.config/retroarch/cores",
                "/usr/lib/libretro",
                "/usr/lib64/libretro",
                "/usr/lib/x86_64-linux-gnu/libretro"
            )
        }
        return (listOfNotNull(configured) + defaults).distinct().map { "$it/$file" }
    }

    fun parseLibretroDirectory(config: String, home: String): String? =
        config.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.substringBefore('=').trim() == "libretro_directory" }
            ?.substringAfter('=')
            ?.trim()
            ?.removeSurrounding("\"")
            ?.takeIf { it.isNotBlank() && it != "default" }
            ?.let { if (it.startsWith("~/")) home + it.substring(1) else it }
            ?.trimEnd('/')

    fun intent(packageName: String, command: List<String>, workingDirectory: String? = null): Intent = Intent().apply {
        intentPackage = packageName
        extras[EXTRA_HOST_COMMAND] = command
        workingDirectory?.let { extras[EXTRA_HOST_DIRECTORY] = it }
    }

    @Suppress("UNCHECKED_CAST")
    fun commandOf(intent: Intent): List<String>? = intent.extras[EXTRA_HOST_COMMAND] as? List<String>

    fun directoryOf(intent: Intent): String? = intent.extras[EXTRA_HOST_DIRECTORY] as? String
}
