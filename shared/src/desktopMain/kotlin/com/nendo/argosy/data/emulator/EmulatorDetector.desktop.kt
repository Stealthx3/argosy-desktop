package com.nendo.argosy.data.emulator

import com.nendo.argosy.data.platform.PlatformDefinitions
import com.nendo.argosy.platform.HostShell
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

@Singleton
actual class EmulatorDetector @Inject constructor() {
    private val _installedEmulators = MutableStateFlow<List<InstalledEmulator>>(emptyList())
    actual val installedEmulators: StateFlow<List<InstalledEmulator>> = _installedEmulators.asStateFlow()

    private val installs = MutableStateFlow<Map<String, LinuxInstall>>(emptyMap())

    actual suspend fun detectEmulators(): List<InstalledEmulator> = withContext(Dispatchers.IO) {
        val output = HostShell.runScript(LinuxEmulators.INVENTORY_SCRIPT, LinuxEmulators.allBinaries)
            ?: return@withContext _installedEmulators.value
        val (home, inventory) = LinuxEmulators.parseInventory(output)
        val installed = mutableListOf<InstalledEmulator>()
        val found = mutableMapOf<String, LinuxInstall>()

        for (linux in LinuxEmulators.all) {
            val def = EmulatorRegistry.getById(linux.id) ?: continue
            if (def.packageName in found) continue
            val install = LinuxEmulators.findInstall(linux, inventory, "$home/Applications") ?: continue
            found[def.packageName] = install
            installed.add(
                InstalledEmulator(
                    def = def.copy(displayName = linux.displayName),
                    versionName = install.version,
                    versionCode = 0
                )
            )
        }

        installs.value = found
        _installedEmulators.value = installed
        installed
    }

    fun linuxInstall(packageName: String): LinuxInstall? = installs.value[packageName]

    actual fun isInstalled(packageName: String): Boolean = packageName in installs.value

    actual fun getInstalledForPlatform(platformId: String): List<InstalledEmulator> {
        val canonical = PlatformDefinitions.getCanonicalSlug(platformId)
        return _installedEmulators.value.filter { canonical in it.def.supportedPlatforms }
    }

    actual fun getPreferredEmulator(platformId: String, builtinEnabled: Boolean): InstalledEmulator? {
        val canonical = PlatformDefinitions.getCanonicalSlug(platformId)
        val installed = getInstalledForPlatform(canonical).let { list ->
            if (builtinEnabled) list else list.filterNot { it.def.id == EmulatorRegistry.BUILTIN_ID }
        }
        if (installed.isEmpty()) return null

        val recommended = EmulatorRegistry.getRecommendedEmulators()[canonical]
        if (recommended != null) {
            val filteredRecommended = if (builtinEnabled) recommended else recommended.filter { it != EmulatorRegistry.BUILTIN_ID }
            for (emulatorId in filteredRecommended) {
                val match = installed.find { it.def.id == emulatorId }
                if (match != null) return match
            }
        }

        val dominated = setOf("retroarch", "retroarch_64", "retroarch_32", "lemuroid")
        val standalone = installed.filterNot { it.def.id in dominated }
        return standalone.firstOrNull() ?: installed.first()
    }

    actual fun hasAnyEmulator(platformId: String): Boolean {
        val canonical = PlatformDefinitions.getCanonicalSlug(platformId)
        return _installedEmulators.value.any { canonical in it.def.supportedPlatforms }
    }

    actual fun getByPackage(packageName: String): EmulatorDef? {
        _installedEmulators.value
            .find { it.def.packageName == packageName }
            ?.let { return it.def }

        return EmulatorRegistry.getByPackage(packageName)
    }
}
