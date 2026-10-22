package com.nendo.argosy.data.emulator

/**
 * Linux installs of an upstream [EmulatorDef], keyed by its id. Flatpak ids are Flathub's;
 * AppImage prefixes and [installPaths] (relative to ~/Applications) follow EmuDeck's layout.
 */
data class LinuxEmulator(
    val id: String,
    val displayName: String,
    val flatpakIds: List<String> = emptyList(),
    val binaries: List<String> = emptyList(),
    val appImagePrefixes: List<String> = emptyList(),
    val installPaths: List<String> = emptyList()
)

enum class LinuxInstallKind { FLATPAK, APPIMAGE, NATIVE }

data class LinuxInstall(
    val kind: LinuxInstallKind,
    val target: String,
    val version: String?
)

data class HostInventory(
    val flatpaks: Map<String, String?>,
    val pathBinaries: Map<String, String>,
    val applications: List<String>
)

object LinuxEmulators {
    val all: List<LinuxEmulator> = listOf(
        LinuxEmulator(
            id = "retroarch",
            displayName = "RetroArch",
            flatpakIds = listOf("org.libretro.RetroArch"),
            binaries = listOf("retroarch"),
            appImagePrefixes = listOf("RetroArch")
        ),
        LinuxEmulator(
            id = "dolphin",
            displayName = "Dolphin",
            flatpakIds = listOf("org.DolphinEmu.dolphin-emu"),
            binaries = listOf("dolphin-emu"),
            appImagePrefixes = listOf("Dolphin")
        ),
        LinuxEmulator(
            id = "cemu",
            displayName = "Cemu",
            flatpakIds = listOf("info.cemu.Cemu"),
            binaries = listOf("cemu", "Cemu"),
            appImagePrefixes = listOf("Cemu")
        ),
        LinuxEmulator(
            id = "azahar",
            displayName = "Azahar",
            flatpakIds = listOf("org.azahar_emu.Azahar"),
            binaries = listOf("azahar"),
            appImagePrefixes = listOf("azahar")
        ),
        LinuxEmulator(
            id = "ryujinx",
            displayName = "Ryujinx",
            flatpakIds = listOf("io.github.ryubing.Ryujinx"),
            binaries = listOf("ryujinx", "Ryujinx"),
            appImagePrefixes = listOf("Ryujinx"),
            installPaths = listOf("publish/Ryujinx")
        ),
        LinuxEmulator(
            id = "eden",
            displayName = "Eden",
            flatpakIds = listOf("dev.eden_emu.eden"),
            binaries = listOf("eden"),
            appImagePrefixes = listOf("Eden")
        ),
        LinuxEmulator(
            id = "melonds",
            displayName = "melonDS",
            flatpakIds = listOf("net.kuribo64.melonDS"),
            binaries = listOf("melonDS"),
            appImagePrefixes = listOf("melonDS")
        ),
        LinuxEmulator(
            id = "duckstation",
            displayName = "DuckStation",
            flatpakIds = listOf("org.duckstation.DuckStation"),
            binaries = listOf("duckstation-qt"),
            appImagePrefixes = listOf("DuckStation")
        ),
        LinuxEmulator(
            id = "pcsx2",
            displayName = "PCSX2",
            flatpakIds = listOf("net.pcsx2.PCSX2"),
            binaries = listOf("pcsx2-qt"),
            appImagePrefixes = listOf("pcsx2")
        ),
        LinuxEmulator(
            id = "ppsspp",
            displayName = "PPSSPP",
            flatpakIds = listOf("org.ppsspp.PPSSPP"),
            binaries = listOf("PPSSPPSDL", "PPSSPPQt", "ppsspp"),
            appImagePrefixes = listOf("PPSSPP")
        ),
        LinuxEmulator(
            id = "vita3k",
            displayName = "Vita3K",
            binaries = listOf("Vita3K"),
            appImagePrefixes = listOf("Vita3K"),
            installPaths = listOf("Vita3K/Vita3K")
        ),
        LinuxEmulator(
            id = "flycast",
            displayName = "Flycast",
            flatpakIds = listOf("org.flycast.Flycast"),
            binaries = listOf("flycast"),
            appImagePrefixes = listOf("flycast")
        ),
        LinuxEmulator(
            id = "scummvm",
            displayName = "ScummVM",
            flatpakIds = listOf("org.scummvm.ScummVM"),
            binaries = listOf("scummvm"),
            appImagePrefixes = listOf("ScummVM")
        ),
        LinuxEmulator(
            id = "mame4droid",
            displayName = "MAME",
            flatpakIds = listOf("org.mamedev.MAME"),
            binaries = listOf("mame")
        ),
        LinuxEmulator(
            id = "dosbox_turbo",
            displayName = "DOSBox",
            flatpakIds = listOf("com.dosbox_x.DOSBox-X", "io.github.dosbox-staging"),
            binaries = listOf("dosbox-x", "dosbox-staging", "dosbox")
        ),
        LinuxEmulator(
            id = "aps3e",
            displayName = "RPCS3",
            flatpakIds = listOf("net.rpcs3.RPCS3"),
            binaries = listOf("rpcs3"),
            appImagePrefixes = listOf("rpcs3")
        ),
        LinuxEmulator(
            id = "mupen64plus_fz",
            displayName = "Rosalie's Mupen GUI",
            flatpakIds = listOf("com.github.Rosalie241.RMG"),
            binaries = listOf("RMG"),
            appImagePrefixes = listOf("RMG")
        )
    )

    val allBinaries: List<String> get() = all.flatMap { it.binaries }.distinct()

    val INVENTORY_SCRIPT = """
echo "${'$'}HOME"
echo @@FLATPAK
flatpak list --app --columns=application,version 2>/dev/null
echo @@PATH
for b in "${'$'}@"; do p=${'$'}(command -v -- "${'$'}b") && printf '%s\t%s\n' "${'$'}b" "${'$'}p"; done
echo @@APPS
find "${'$'}HOME/Applications" -maxdepth 2 \( -type f -o -type l \) -perm -u+x 2>/dev/null
"""

    fun parseInventory(output: String): Pair<String, HostInventory> {
        val lines = output.lines()
        val home = lines.firstOrNull()?.trim().orEmpty()
        fun section(name: String): List<String> {
            val start = lines.indexOf("@@$name")
            if (start < 0) return emptyList()
            return lines.drop(start + 1).takeWhile { !it.startsWith("@@") }.filter { it.isNotBlank() }
        }
        val pathBinaries = section("PATH").mapNotNull { line ->
            val columns = line.split('\t')
            if (columns.size == 2) columns[0] to columns[1] else null
        }.toMap()
        return home to HostInventory(
            flatpaks = parseFlatpakList(section("FLATPAK").joinToString("\n")),
            pathBinaries = pathBinaries,
            applications = section("APPS").map { it.trim() }
        )
    }

    fun parseFlatpakList(output: String): Map<String, String?> =
        output.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .associate { line ->
                val columns = line.split('\t')
                columns[0].trim() to columns.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
            }

    fun findInstall(emulator: LinuxEmulator, inventory: HostInventory, applicationsDir: String): LinuxInstall? {
        emulator.flatpakIds.firstOrNull { it in inventory.flatpaks }?.let { id ->
            return LinuxInstall(LinuxInstallKind.FLATPAK, id, inventory.flatpaks[id])
        }
        val appImage = emulator.appImagePrefixes.firstNotNullOfOrNull { prefix ->
            inventory.applications.firstOrNull { path ->
                val name = path.substringAfterLast('/')
                name.startsWith(prefix, ignoreCase = true) && name.endsWith(".AppImage", ignoreCase = true)
            }
        }
        if (appImage != null) return LinuxInstall(LinuxInstallKind.APPIMAGE, appImage, null)
        emulator.installPaths.firstNotNullOfOrNull { relative ->
            inventory.applications.firstOrNull { it == "$applicationsDir/$relative" }
        }?.let { return LinuxInstall(LinuxInstallKind.NATIVE, it, null) }
        return emulator.binaries.firstNotNullOfOrNull { inventory.pathBinaries[it] }
            ?.let { LinuxInstall(LinuxInstallKind.NATIVE, it, null) }
    }
}
