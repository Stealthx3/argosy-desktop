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
    val installPaths: List<String> = emptyList(),
    val launch: LinuxLaunchArgs? = null,
    val flathubId: String? = null
)

data class LinuxLaunchArgs(val args: List<String>, val onlyTargetsContaining: String? = null)

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
    val all: List<LinuxEmulator> get() = LinuxEmulatorDatabase.current.emulators

    fun byId(id: String): LinuxEmulator? = all.firstOrNull { it.id == id }

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
