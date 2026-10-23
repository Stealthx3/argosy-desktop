package com.nendo.argosy.data.emulator

import com.nendo.argosy.data.local.dao.EmulatorConfigDao
import com.nendo.argosy.data.local.dao.GameDao
import com.nendo.argosy.data.local.entity.GameEntity
import com.nendo.argosy.data.model.GameSource
import com.nendo.argosy.data.preferences.AccountSwitchMarkerStore
import com.nendo.argosy.data.repository.GameUserOverlayWriter
import com.nendo.argosy.data.storage.StorageVolumeHealth
import com.nendo.argosy.platform.HostGameProcess
import com.nendo.argosy.platform.HostShell
import com.nendo.argosy.platform.PlatformIntentType as Intent
import com.nendo.argosy.util.Logger
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "GameLauncher"

/**
 * Launches a single-file game on a host emulator found by [EmulatorDetector]. The emulator comes
 * from [EmulatorResolver.getEmulatorPackageForGame], so the launcher and every screen that names
 * the game's emulator agree. Variants, multi-disc games, Steam, GameNative and Android apps are
 * not launched on desktop yet.
 */
@Singleton
actual class GameLauncher @Inject constructor(
    private val gameDao: GameDao,
    private val emulatorConfigDao: EmulatorConfigDao,
    private val emulatorResolver: EmulatorResolver,
    private val emulatorDetector: EmulatorDetector,
    private val overlayWriter: GameUserOverlayWriter,
    private val accountSwitchMarkerStore: AccountSwitchMarkerStore,
    private val baseRomFileResolver: BaseRomFileResolver,
    private val volumeHealth: StorageVolumeHealth
) {
    actual suspend fun launch(gameId: Long, discId: Long?, forResume: Boolean, selectedDiscPath: String?, variantFileId: Long?, skipVariantPrompt: Boolean, allowVariantPrompt: Boolean, prefetchedGame: GameEntity?): LaunchResult = withContext(Dispatchers.IO) {
        launchInternal(gameId, forResume, prefetchedGame)
    }

    private suspend fun launchInternal(gameId: Long, forResume: Boolean, prefetchedGame: GameEntity?): LaunchResult {
        if (accountSwitchMarkerStore.isSwitching()) {
            Logger.warn(TAG, "launch() refused: an account switch is in progress")
            return LaunchResult.Error("Finishing the account switch - try again in a moment")
        }

        val game = prefetchedGame ?: gameDao.getById(gameId)
            ?: return LaunchResult.Error("Game not found")

        when {
            game.source == GameSource.STEAM -> return LaunchResult.Error("Steam games cannot be launched on desktop yet")
            game.source == GameSource.GAMENATIVE -> return LaunchResult.Error("GameNative games cannot be launched on desktop")
            game.source == GameSource.ANDROID_APP || game.platformSlug == "android" ->
                return LaunchResult.NoAndroidApp(game.packageName ?: game.title)
            game.isMultiDisc -> return LaunchResult.Error("Multi-disc games cannot be launched on desktop yet")
        }

        val romPath = game.localPath ?: return LaunchResult.NoRomFile(null)
        var romFile = File(romPath)
        if (!romFile.exists()) {
            if (volumeHealth.newProbe().isGenuinelyAbsent(romPath)) {
                gameDao.clearLocalPath(game.id)
            }
            return LaunchResult.NoRomFile(romPath)
        }
        romFile = baseRomFileResolver.resolve(game, romFile)

        val packageName = emulatorResolver.getEmulatorPackageForGame(game.id, game.platformId, game.platformSlug)
        val emulator = packageName?.let { emulatorDetector.getByPackage(it) }
        val install = packageName?.let { emulatorDetector.linuxInstall(it) }
        if (emulator == null || install == null) {
            Logger.warn(TAG, "launch() failed: no host emulator for platform=${game.platformSlug} (package=$packageName)")
            return LaunchResult.NoEmulator(game.platformSlug)
        }

        val command = when (emulator.launchConfig) {
            is LaunchConfig.RetroArch -> {
                val coreName = resolveCoreName(game) ?: return LaunchResult.NoCore(game.platformSlug)
                val corePath = findRetroArchCore(install, coreName)
                    ?: return LaunchResult.NoCore(game.platformSlug, "${coreName}_libretro.so is not installed in RetroArch")
                LinuxLaunch.retroArch(install, corePath, romFile.absolutePath)
            }
            else -> LinuxLaunch.standalone(emulator.id, install, romFile.absolutePath)
        } ?: return LaunchResult.Error("Failed to launch ${emulator.displayName}").also {
            Logger.error(TAG, "launch() failed: no verified command line for ${emulator.id} (${install.kind})")
        }

        if (!forResume) {
            overlayWriter.recordPlayStart(gameId, Instant.now())
        }
        Logger.info(TAG, "[Launch] ${romFile.name} via ${emulator.displayName} | gameId=$gameId | platform=${game.platformSlug} | ${install.kind} ${install.target}")
        return LaunchResult.Success(LinuxLaunch.intent(emulator.packageName, command))
    }

    private fun findRetroArchCore(install: LinuxInstall, coreName: String): String? {
        val home = System.getProperty("user.home")
        val config = HostShell.run(listOf("cat", LinuxLaunch.retroArchConfigPath(install, home)))
        val candidates = LinuxLaunch.retroArchCoreCandidates(install, home, coreName, config)
        val found = HostShell.runScript(
            "for f in \"$@\"; do [ -f \"\$f\" ] && { printf '%s' \"\$f\"; exit 0; }; done",
            candidates
        )
        return found?.takeIf { it.isNotBlank() }
    }

    private suspend fun resolveCoreName(game: GameEntity): String? {
        val configured = emulatorConfigDao.getByGameId(game.id)?.coreName
            ?: emulatorConfigDao.getDefaultForPlatform(game.platformId)?.coreName
        if (configured != null) return normalizeLegacyCoreName(configured, game.platformSlug)
        return EmulatorRegistry.getDefaultCore(game.platformSlug)?.id
            ?: EmulatorRegistry.getPreferredCore(game.platformSlug)
    }

    private fun normalizeLegacyCoreName(coreName: String, platformSlug: String): String {
        val externalCores = EmulatorRegistry.getSelectableCores(platformSlug, isBuiltIn = false)
        if (externalCores.any { it.id == coreName }) return coreName
        val isBuiltinCore = com.nendo.argosy.libretro.LibretroCoreRegistry
            .getCoresForPlatform(platformSlug).any { it.coreId == coreName }
        if (isBuiltinCore) {
            EmulatorRegistry.getDefaultSelectableCore(platformSlug, isBuiltIn = false)?.id?.let { return it }
        }
        return externalCores.find { it.id.startsWith(coreName) }?.id ?: coreName
    }

    actual suspend fun buildInstallIntent(game: GameEntity, file: File): Intent? = null

    actual suspend fun buildBatchInstallIntent(game: GameEntity, files: List<File>): Intent? = null

    actual suspend fun forceStopEmulator(packageName: String) {
        HostGameProcess.stop(packageName)
    }
}
