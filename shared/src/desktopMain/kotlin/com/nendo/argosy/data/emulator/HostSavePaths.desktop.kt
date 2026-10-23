package com.nendo.argosy.data.emulator

import com.nendo.argosy.data.storage.DesktopFileAccessLayer
import java.io.File

/**
 * Linux save folders of the standalone emulators [LinuxEmulators] detects, keyed by
 * [SavePathConfig.emulatorId]. Each emulator's own default user folder (XDG data or config, and
 * its Flathub `~/.var/app/<id>` tree) keeps the layout below it that the Android template names.
 * EmuDeck's `~/Emulation/saves/<emu>` links follow the folders EmuDeck configured, so they come
 * first, then the Flatpak tree (detection's Flatpak > AppImage > PATH order), then the native one.
 * Folders that exist come before ones that do not, keeping that order within each.
 * melonDS (saves and states beside the ROM) and Rosalie's Mupen GUI (no GameData tree) are left
 * out. State folders cover the [StatePathRegistry] configs whose emulator runs on Linux.
 * Any other config keeps only its app-private templates (`{filesDir}`, `{builtinSaves}`) and its
 * RetroArch ones, whose `RetroArch/saves` and `RetroArch/states` roots become the host RetroArch's
 * own folders; a phone folder (`~/Android/data/...`, `~/DraStic/...`, `/data/data/...`) names nothing here,
 * and a save download would otherwise create it in the home folder.
 */
actual object HostSavePaths {
    private val home: String get() = System.getProperty("user.home")

    private fun templates(configId: String): List<String>? = when (configId) {
        "dolphin" -> dolphin("GC")
        "dolphin_wii" -> dolphin("Wii/title/00010000")
        "cemu" -> folders(
            "Cemu/saves/00050000",
            "info.cemu.Cemu", "data/Cemu/mlc01/usr/save/00050000",
            ".local/share/Cemu/mlc01/usr/save/00050000"
        )
        "azahar" -> folders(
            "azahar/saves/Nintendo 3DS",
            "org.azahar_emu.Azahar", "data/azahar-emu/sdmc/Nintendo 3DS",
            ".local/share/azahar-emu/sdmc/Nintendo 3DS"
        )
        "ryujinx" -> folders(
            "ryujinx/saves",
            "io.github.ryubing.Ryujinx", "config/Ryujinx/bis/user/save",
            ".config/Ryujinx/bis/user/save"
        )
        "eden" -> folders(
            null,
            "dev.eden_emu.eden", "data/eden/nand/user/save",
            ".local/share/eden/nand/user/save"
        )
        "duckstation" -> folders(
            "duckstation/saves",
            "org.duckstation.DuckStation", "data/duckstation/memcards",
            ".local/share/duckstation/memcards"
        )
        "pcsx2" -> folders(
            "pcsx2/saves",
            "net.pcsx2.PCSX2", "config/PCSX2/memcards",
            ".config/PCSX2/memcards"
        )
        "ppsspp" -> folders(
            "ppsspp/saves",
            "org.ppsspp.PPSSPP", "config/ppsspp/PSP/SAVEDATA",
            ".config/ppsspp/PSP/SAVEDATA"
        )
        "vita3k" -> folders(
            "Vita3K/saves",
            null, null,
            ".local/share/Vita3K/Vita3K/ux0/user/00/savedata"
        )
        "aps3e" -> folders(
            "rpcs3/saves",
            "net.rpcs3.RPCS3", "config/rpcs3/dev_hdd0/home/00000001/savedata",
            ".config/rpcs3/dev_hdd0/home/00000001/savedata"
        )
        else -> null
    }

    private fun stateTemplates(configId: String): List<String>? = when (configId) {
        "dolphin" -> dolphin("StateSaves")
        "duckstation" -> folders(
            "duckstation/states",
            "org.duckstation.DuckStation", "data/duckstation/savestates",
            ".local/share/duckstation/savestates"
        )
        "ppsspp" -> folders(
            "ppsspp/states",
            "org.ppsspp.PPSSPP", "config/ppsspp/PSP/PPSSPP_STATE",
            ".config/ppsspp/PSP/PPSSPP_STATE"
        )
        else -> null
    }

    private fun dolphin(tree: String) = folders(
        "dolphin/$tree",
        "org.DolphinEmu.dolphin-emu", "data/dolphin-emu/$tree",
        ".local/share/dolphin-emu/$tree", ".dolphin-emu/$tree"
    )

    private fun folders(emuDeck: String?, flatpakId: String?, flatpakTree: String?, vararg native: String) =
        buildList {
            emuDeck?.let { add("$home/Emulation/saves/$it") }
            if (flatpakId != null && flatpakTree != null) add("$home/.var/app/$flatpakId/$flatpakTree")
            native.forEach { add("$home/$it") }
        }

    private fun existingFirst(paths: List<String>): List<String> {
        val (present, absent) = paths.partition { File(it).isDirectory }
        return present + absent
    }

    private fun retroArchDirectory(name: String): String {
        val cfg = RetroArchHostLayout.configPaths("", emptyList()).map(::File).firstOrNull { it.isFile }
        val parser = RetroArchConfigParser(DesktopFileAccessLayer())
        val configured = when (name) {
            "saves" -> cfg?.let { parser.parseFile(it).savefileDirectory }
            else -> cfg?.let { parser.parseStateConfigFromFile(it).savestateDirectory }
        }
        return configured ?: RetroArchHostLayout.defaultDirectory("", name)
    }

    private fun hostTemplates(templates: List<String>): List<String> = templates.mapNotNull { template ->
        RETROARCH_ROOTS.firstNotNullOfOrNull { name ->
            val root = "$EXT_STORAGE/RetroArch/$name"
            if (template == root || template.startsWith("$root/")) {
                retroArchDirectory(name) + template.removePrefix(root)
            } else {
                null
            }
        } ?: template.takeIf { t -> APP_PRIVATE.any { t.startsWith(it) } }
    }.distinct()

    actual fun saveDirectories(configId: String, templates: List<String>): List<String> =
        templates(configId)?.let(::existingFirst) ?: hostTemplates(templates)

    actual fun stateDirectories(configId: String, templates: List<String>): List<String> =
        stateTemplates(configId)?.let(::existingFirst) ?: hostTemplates(templates)

    private const val EXT_STORAGE = "{extStorage}"
    private val APP_PRIVATE = listOf("{filesDir}", BUILTIN_SAVES_TOKEN)
    private val RETROARCH_ROOTS = listOf("saves", "states")
}
