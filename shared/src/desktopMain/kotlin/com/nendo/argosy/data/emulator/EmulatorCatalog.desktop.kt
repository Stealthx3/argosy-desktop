package com.nendo.argosy.data.emulator

import com.nendo.argosy.data.model.CatalogEmulator
import com.nendo.argosy.data.model.CatalogPackage
import com.nendo.argosy.platform.DesktopDirectories
import com.nendo.argosy.platform.HostShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class EmulatorCatalog @Inject constructor(
    private val emulatorDetector: EmulatorDetector
) {
    actual val isSupported: Boolean = true

    private val home: String get() = System.getProperty("user.home")

    actual suspend fun entries(): List<CatalogEmulator> = withContext(Dispatchers.IO) {
        emulatorDetector.detectEmulators()
        val launchers = HostShell.runScript(EmulatorCatalogRules.LAUNCHER_SCRIPT, listOf("$home/${EmulatorCatalogRules.EMUDECK_LAUNCHERS}")).orEmpty()
        val emuDeckPresent = HostShell.runScript(EmulatorCatalogRules.EMUDECK_PRESENT_SCRIPT, listOf(home)).orEmpty().trim() == "yes"
        LinuxEmulators.all.mapNotNull { entry ->
            val def = EmulatorRegistry.getById(entry.id) ?: return@mapNotNull null
            val install = emulatorDetector.linuxInstall(def.packageName)
            val emuDeck = EmulatorCatalogRules.emuDeckManaged(entry, install, launchers)
            CatalogEmulator(
                id = entry.id,
                displayName = entry.displayName,
                platforms = def.supportedPlatforms.toList(),
                installed = install?.let { EmulatorCatalogRules.packageOf(it.kind) },
                version = install?.version,
                emuDeckManaged = emuDeck,
                installOptions = EmulatorCatalogRules.installOptions(entry),
                dataFolder = install?.let { EmulatorCatalogRules.dataFolder(it, home) },
                biosFolder = EmulatorCatalogRules.biosFolder(
                    entry = entry,
                    install = install,
                    home = home,
                    emuDeckBios = emuDeck && emuDeckPresent,
                    retroArchSystemDir = if (entry.id == EmulatorCatalogRules.RETROARCH_ID && install != null) retroArchSystemDir(install) else null
                ),
                logFolder = emulatorLogFolder(),
                appImageRepo = entry.appImageRepo
            )
        }
    }

    actual suspend fun installFlatpak(id: String): Boolean = withContext(Dispatchers.IO) {
        val flathubId = LinuxEmulators.all.firstOrNull { it.id == id }?.flathubId ?: return@withContext false
        HostShell.run(FlathubSuggestions.ADD_REMOTE)
        HostShell.run(FlathubSuggestions.installArgs(flathubId))
        emulatorDetector.detectEmulators()
        EmulatorRegistry.getById(id)?.let { emulatorDetector.isInstalled(it.packageName) } == true
    }

    actual suspend fun uninstall(id: String): Boolean = withContext(Dispatchers.IO) {
        val def = EmulatorRegistry.getById(id) ?: return@withContext false
        val install = emulatorDetector.linuxInstall(def.packageName) ?: return@withContext false
        val command = EmulatorCatalogRules.uninstallCommand(install) ?: return@withContext false
        HostShell.run(command)
        emulatorDetector.detectEmulators()
        !emulatorDetector.isInstalled(def.packageName)
    }

    actual fun openFolder(path: String) {
        HostShell.run(listOf("mkdir", "-p", path))
        HostShell.spawn(listOf("xdg-open", path), null)
    }

    actual suspend fun copyBios(sourcePath: String, folder: String): Boolean = withContext(Dispatchers.IO) {
        HostShell.runScript(EmulatorCatalogRules.COPY_SCRIPT, listOf(sourcePath, folder)).orEmpty().trim() == "ok"
    }

    private fun retroArchSystemDir(install: LinuxInstall): String? =
        HostShell.run(listOf("cat", LinuxLaunch.retroArchConfigPath(install, home)))
            ?.let { EmulatorCatalogRules.retroArchSystemDirectory(it, home) }

    private fun emulatorLogFolder(): String = DesktopDirectories.data.resolve(EmulatorCatalogRules.LOG_FOLDER).path
}

/**
 * The host-independent rules behind [EmulatorCatalog]: package kinds, folders, EmuDeck detection
 * and uninstall commands.
 */
object EmulatorCatalogRules {
    const val RETROARCH_ID = "retroarch"
    const val EMUDECK_LAUNCHERS = "Emulation/tools/launchers"
    const val EMUDECK_BIOS = "Emulation/bios"
    const val EMUDECK_CONFIG = ".config/EmuDeck"
    const val LOG_FOLDER = "logs/emulators"
    const val LAUNCHER_SCRIPT = "cat \"\$1\"/*.sh \"\$1\"/*/*.sh 2>/dev/null"
    const val EMUDECK_PRESENT_SCRIPT = "[ -d \"\$1/$EMUDECK_BIOS\" ] && [ -d \"\$1/$EMUDECK_CONFIG\" ] && echo yes"
    const val COPY_SCRIPT = "mkdir -p \"\$2\" && cp -f \"\$1\" \"\$2\"/ && echo ok"
    private const val RETROARCH_FLATPAK = "org.libretro.RetroArch"
    private const val SYSTEM_DIRECTORY_KEY = "system_directory"
    private const val DEFAULT_VALUE = "default"

    fun packageOf(kind: LinuxInstallKind): CatalogPackage = when (kind) {
        LinuxInstallKind.FLATPAK -> CatalogPackage.FLATPAK
        LinuxInstallKind.APPIMAGE -> CatalogPackage.APPIMAGE
        LinuxInstallKind.NATIVE -> CatalogPackage.NATIVE
    }

    fun installOptions(entry: LinuxEmulator): List<CatalogPackage> = listOfNotNull(
        CatalogPackage.FLATPAK.takeIf { entry.flathubId != null },
        CatalogPackage.APPIMAGE.takeIf { entry.appImageRepo != null }
    )

    fun emuDeckManaged(entry: LinuxEmulator, install: LinuxInstall?, launcherText: String): Boolean {
        if (install == null || launcherText.isBlank()) return false
        val marks = entry.flatpakIds + entry.appImagePrefixes
        return marks.any { launcherText.contains(it, ignoreCase = true) }
    }

    fun dataFolder(install: LinuxInstall, home: String): String? =
        if (install.kind == LinuxInstallKind.FLATPAK) "$home/.var/app/${install.target}" else null

    fun uninstallCommand(install: LinuxInstall): List<String>? = when (install.kind) {
        LinuxInstallKind.FLATPAK -> listOf("flatpak", "uninstall", "-y", "--noninteractive", install.target)
        LinuxInstallKind.APPIMAGE -> listOf("rm", "-f", install.target)
        LinuxInstallKind.NATIVE -> null
    }

    fun biosFolder(
        entry: LinuxEmulator,
        install: LinuxInstall?,
        home: String,
        emuDeckBios: Boolean,
        retroArchSystemDir: String?
    ): String? = when {
        install == null -> null
        emuDeckBios -> "$home/$EMUDECK_BIOS"
        entry.id == RETROARCH_ID -> retroArchSystemDir ?: defaultRetroArchSystemDir(install, home)
        else -> null
    }

    fun retroArchSystemDirectory(config: String, home: String): String? =
        config.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith(SYSTEM_DIRECTORY_KEY) && it.substringBefore('=').trim() == SYSTEM_DIRECTORY_KEY }
            ?.substringAfter('=')
            ?.trim()
            ?.trim('"')
            ?.takeIf { it.isNotEmpty() && it != DEFAULT_VALUE }
            ?.replaceFirst(Regex("^~"), home)

    private fun defaultRetroArchSystemDir(install: LinuxInstall, home: String): String =
        if (install.kind == LinuxInstallKind.FLATPAK && install.target == RETROARCH_FLATPAK) {
            "$home/.var/app/$RETROARCH_FLATPAK/config/retroarch/system"
        } else {
            "$home/.config/retroarch/system"
        }
}
