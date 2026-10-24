package com.nendo.argosy.data.emulator

import com.nendo.argosy.platform.HostShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class EmulatorSuggestions @Inject constructor(
    private val emulatorDetector: EmulatorDetector
) {
    actual val isSupported: Boolean = true

    actual fun forPlatform(canonicalSlug: String, installedPackages: Set<String>): List<EmulatorDef> =
        FlathubSuggestions.forPlatform(LinuxEmulators.all, canonicalSlug, installedPackages)

    actual suspend fun install(emulator: EmulatorDef): Boolean = withContext(Dispatchers.IO) {
        val flathubId = LinuxEmulators.all.firstOrNull { it.id == emulator.id && it.flathubId != null }?.flathubId
            ?: return@withContext false
        HostShell.run(FlathubSuggestions.ADD_REMOTE)
        HostShell.run(FlathubSuggestions.installArgs(flathubId))
        emulatorDetector.detectEmulators()
        emulatorDetector.isInstalled(emulator.packageName)
    }
}

object FlathubSuggestions {
    const val PAGE = "https://flathub.org/apps/"

    val ADD_REMOTE = listOf(
        "flatpak", "remote-add", "--user", "--if-not-exists", "flathub", "https://dl.flathub.org/repo/flathub.flatpakrepo"
    )

    fun installArgs(flathubId: String): List<String> =
        listOf("flatpak", "install", "--user", "-y", "--noninteractive", "flathub", flathubId)

    fun forPlatform(entries: List<LinuxEmulator>, canonicalSlug: String, installedPackages: Set<String>): List<EmulatorDef> =
        entries.mapNotNull { entry ->
            val flathubId = entry.flathubId ?: return@mapNotNull null
            val def = EmulatorRegistry.getById(entry.id) ?: return@mapNotNull null
            if (canonicalSlug !in def.supportedPlatforms || def.packageName in installedPackages) return@mapNotNull null
            def.copy(displayName = entry.displayName, downloadUrl = PAGE + flathubId, releaseSource = null)
        }.distinctBy { it.packageName }
}
