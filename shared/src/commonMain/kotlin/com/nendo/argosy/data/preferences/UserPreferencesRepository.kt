package com.nendo.argosy.data.preferences

import com.nendo.argosy.data.cache.GradientPreset
import com.nendo.argosy.data.emulator.LaunchOrigin
import com.nendo.argosy.core.input.SoundConfig
import com.nendo.argosy.core.input.SoundType
import com.nendo.argosy.domain.model.ScreenLayouts
import com.nendo.argosy.util.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private data class PreferencesSources(
    val display: DisplayPreferences,
    val sync: SyncPreferences,
    val controls: ControlsPreferences,
    val storage: StoragePreferences,
    val app: AppPreferences
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val displayPrefs: DisplayPreferencesRepository,
    private val syncPrefs: SyncPreferencesRepository,
    private val controlsPrefs: ControlsPreferencesRepository,
    private val storagePrefs: StoragePreferencesRepository,
    private val appPrefs: AppPreferencesRepository,
    private val builtinPrefs: BuiltinEmulatorPreferencesRepository,
    private val sessionPrefs: SessionPreferencesRepository,
    private val jellyfinPrefs: JellyfinPreferencesRepository
) {
    private val shareScope = com.nendo.argosy.util.SafeCoroutineScope(Dispatchers.Default, "UserPreferences")

    private val preferencesSource: Flow<UserPreferences> = combine(
        combine(
            displayPrefs.preferences,
            syncPrefs.preferences,
            controlsPrefs.preferences,
            storagePrefs.preferences,
            appPrefs.preferences
        ) { display, sync, controls, storage, app ->
            PreferencesSources(display, sync, controls, storage, app)
        },
        builtinPrefs.isBuiltinLibretroEnabled(),
        jellyfinPrefs.preferences
    ) { sources, builtinEnabled, jellyfin ->
        val (display, sync, controls, storage, app) = sources
        UserPreferences(
            firstRunComplete = app.firstRunComplete,
            rommBaseUrl = sync.rommBaseUrl,
            rommUsername = sync.rommUsername,
            rommUserId = sync.rommUserId,
            rommAvatarPath = sync.rommAvatarPath,
            rommToken = sync.rommToken,
            rommDeviceId = sync.rommDeviceId,
            rommDeviceClientVersion = sync.rommDeviceClientVersion,
            raUsername = sync.raUsername,
            raToken = sync.raToken,
            raWebApiKey = sync.raWebApiKey,
            raProxyEnabled = sync.raProxyEnabled,
            raProxyAddress = sync.raProxyAddress,
            romStoragePath = storage.romStoragePath,
            showStatusClock = display.showStatusClock,
            showStatusBattery = display.showStatusBattery,
            showStatusNetwork = display.showStatusNetwork,
            themeMode = display.themeMode,
            primaryColor = display.primaryColor,
            secondaryColor = display.secondaryColor,
            tertiaryColor = display.tertiaryColor,
            surfaceTintBleed = display.surfaceTintBleed,
            backdropEnabled = display.backdropEnabled,
            backdropPreset = display.backdropPreset,
            backdropCellSize = display.backdropCellSize,
            backdropScatter = display.backdropScatter,
            backdropScaleJitter = display.backdropScaleJitter,
            backdropStrength = display.backdropStrength,
            backdropEdgeStyle = display.backdropEdgeStyle,
            backdropVertexIcons = display.backdropVertexIcons,
            backdropSeed = display.backdropSeed,
            backdropMotion = display.backdropMotion,
            backdropMotionSpeed = display.backdropMotionSpeed,
            backdropDriftAngle = display.backdropDriftAngle,
            displayFontPath = display.displayFontPath,
            displayFontName = display.displayFontName,
            bodyFontPath = display.bodyFontPath,
            bodyFontName = display.bodyFontName,
            displayFontScale = display.displayFontScale,
            bodyFontScale = display.bodyFontScale,
            hapticEnabled = controls.hapticEnabled,
            hapticStrength = controls.hapticStrength,
            soundEnabled = controls.soundEnabled,
            soundVolume = controls.soundVolume,
            swapAB = controls.swapAB,
            swapXY = controls.swapXY,
            controllerLayout = controls.controllerLayout,
            swapStartSelect = controls.swapStartSelect,
            selectLCombo = controls.selectLCombo,
            selectRCombo = controls.selectRCombo,
            menuWrapMode = controls.menuWrapMode,
            controllerInputMode = controls.controllerInputMode,
            selectSwapMode = controls.selectSwapMode,
            openNavigationKey = controls.openNavigationKey,
            openQuickPanelKey = controls.openQuickPanelKey,
            navRingRoutes = controls.navRingRoutes,
            quickNavigation = controls.quickNavigation,
            navBarAutoHide = controls.navBarAutoHide,
            lastRommSync = sync.lastRommSync,
            lastFavoritesSync = sync.lastFavoritesSync,
            lastFavoritesCheck = sync.lastFavoritesCheck,
            syncFilters = sync.syncFilters,
            regionPriority = sync.regionPriority,
            syncScreenshotsEnabled = sync.syncScreenshotsEnabled,
            uploadScreenshotsEnabled = sync.uploadScreenshotsEnabled,
            boxArtCacheEnabled = sync.boxArtCacheEnabled,
            backgroundBlur = display.backgroundBlur,
            backgroundSaturation = display.backgroundSaturation,
            backgroundOpacity = display.backgroundOpacity,
            useGameBackground = display.useGameBackground,
            customBackgroundPath = display.customBackgroundPath,
            homeBackgroundMode = display.homeBackgroundMode,
            homeLayout = display.homeLayout,
            presentationStyle = display.presentationStyle,
            presentationBackgroundBlur = display.presentationBackgroundBlur,
            useAccentColorFooter = display.useAccentColorFooter,
            compactFooter = display.compactFooter,
            hideSystemNavigationBar = display.hideSystemNavigationBar,
            hideSystemStatusBar = display.hideSystemStatusBar,
            lockScreenArt = display.lockScreenArt,
            homeAppAvailable = display.homeAppAvailable,
            iconPack = display.iconPack,
            hiddenApps = app.hiddenApps,
            secondaryHomeApps = app.secondaryHomeApps,
            visibleSystemApps = app.visibleSystemApps,
            appOrder = app.appOrder,
            maxConcurrentDownloads = storage.maxConcurrentDownloads,
            allowRemoteInstalls = storage.allowRemoteInstalls,
            autoStartDownloads = storage.autoStartDownloads,
            instantDownloadThresholdMb = storage.instantDownloadThresholdMb,
            stageDownloadsInternally = storage.stageDownloadsInternally,
            folderNameFromRom = storage.folderNameFromRom,
            gridDensity = display.gridDensity,
            libraryLayout = display.libraryLayout,
            libraryBoxArt3d = display.libraryBoxArt3d,
            libraryDefaultSort = display.libraryDefaultSort,
            libraryDefaultSortDescending = display.libraryDefaultSortDescending,
            sortInstalledFirst = display.sortInstalledFirst,
            sortFavoritesFirst = display.sortFavoritesFirst,
            libraryDefaultSource = display.libraryDefaultSource,
            libraryDefaultPlatformId = display.libraryDefaultPlatformId,
            libraryDefaultRegions = display.libraryDefaultRegions,
            libraryDefaultPlayers = display.libraryDefaultPlayers,
            soundConfigs = controls.soundConfigs,
            betaUpdatesEnabled = app.betaUpdatesEnabled,
            closeEmulatorOnSessionEnd = app.closeEmulatorOnSessionEnd,
            appLanguage = app.appLanguage,
            preLaunchRecapsEnabled = app.preLaunchRecapsEnabled,
            monthlyRecapEnabled = app.monthlyRecapEnabled,
            lastMonthlyRecapShown = app.lastMonthlyRecapShown,
            preLaunchRecapBaseline = app.preLaunchRecapBaseline,
            saveSyncEnabled = sync.saveSyncEnabled,
            secureSaves = sync.secureSaves,
            stateCacheEnabled = sync.stateCacheEnabled,
            saveCacheLimit = sync.saveCacheLimit,
            fileLoggingEnabled = app.fileLoggingEnabled,
            fileLoggingPath = app.fileLoggingPath,
            fileLogLevel = app.fileLogLevel,
            saveDebugLoggingEnabled = sync.saveDebugLoggingEnabled,
            saveWatcherEnabled = sync.saveWatcherEnabled,
            boxArtShape = display.boxArtShape,
            boxArtCornerRadius = display.boxArtCornerRadius,
            boxArtBorderThickness = display.boxArtBorderThickness,
            boxArtBorderStyle = display.boxArtBorderStyle,
            glassBorderTint = display.glassBorderTint,
            boxArtGlowStrength = display.boxArtGlowStrength,
            boxArtOuterEffect = display.boxArtOuterEffect,
            boxArtOuterEffectThickness = display.boxArtOuterEffectThickness,
            glowColorMode = display.glowColorMode,
            boxArtInnerEffect = display.boxArtInnerEffect,
            boxArtInnerEffectThickness = display.boxArtInnerEffectThickness,
            gradientPreset = display.gradientPreset,
            gradientAdvancedMode = display.gradientAdvancedMode,
            systemIconPosition = display.systemIconPosition,
            systemIconPadding = display.systemIconPadding,
            platformIndicatorStyle = display.platformIndicatorStyle,
            platformIndicatorContent = display.platformIndicatorContent,
            defaultView = display.defaultView,
            recommendedGameIds = app.recommendedGameIds,
            lastRecommendationGeneration = app.lastRecommendationGeneration,
            recommendationPenalties = app.recommendationPenalties,
            lastPenaltyDecayWeek = app.lastPenaltyDecayWeek,
            lastSeenVersion = app.lastSeenVersion,
            libraryRecentSearches = app.libraryRecentSearches,
            ambientAudioEnabled = controls.ambientAudioEnabled,
            ambientAudioVolume = controls.ambientAudioVolume,
            ambientAudioUri = controls.ambientAudioUri,
            ambientAudioShuffle = controls.ambientAudioShuffle,
            ambientAudioPlayInBackground = controls.ambientAudioPlayInBackground,
            gameDetailThemeEnabled = controls.gameDetailThemeEnabled,
            imageCachePath = sync.imageCachePath,
            screenDimmerEnabled = display.screenDimmerEnabled,
            screenDimmerTimeoutMinutes = display.screenDimmerTimeoutMinutes,
            screenDimmerLevel = display.screenDimmerLevel,
            customBiosPath = storage.customBiosPath,
            weeklyIntegrityCheckEnabled = storage.weeklyIntegrityCheckEnabled,
            lastIntegrityCheckTime = storage.lastIntegrityCheckTime,
            steamInstallVolume = storage.steamInstallVolume,
            videoWallpaperEnabled = display.videoWallpaperEnabled,
            videoWallpaperDelaySeconds = display.videoWallpaperDelaySeconds,
            videoWallpaperMuted = display.videoWallpaperMuted,
            uiScale = display.uiScale,
            gripReserveMode = display.gripReserveMode,
            gripReservePercent = display.gripReservePercent,
            gripAutoControllers = display.gripAutoControllers,
            ambientLedEnabled = display.ambientLedEnabled,
            ambientLedBrightness = display.ambientLedBrightness,
            ambientLedAudioBrightness = display.ambientLedAudioBrightness,
            ambientLedAudioColors = display.ambientLedAudioColors,
            ambientLedColorMode = display.ambientLedColorMode,
            ambientLedCoverArtEnabled = display.ambientLedCoverArtEnabled,
            ambientLedCustomColor = display.ambientLedCustomColor,
            ambientLedCustomColorHue = display.ambientLedCustomColorHue,
            ambientLedScreenEnabled = display.ambientLedScreenEnabled,
            ambientLedTransitionMs = display.ambientLedTransitionMs,
            ambientLedAchievementFlash = display.ambientLedAchievementFlash,
            builtinLibretroEnabled = builtinEnabled,
            appDisplayTargets = app.appDisplayTargets,
            dualScreenEnabled = display.dualScreenEnabled,
            displayRoleOverride = display.displayRoleOverride,
            gamesDisplayTarget = display.gamesDisplayTarget,
            appsDisplayTarget = display.appsDisplayTarget,
            dualScreenInputFocus = display.dualScreenInputFocus,
            screenLayouts = display.screenLayouts,
            installedOnlyHome = display.installedOnlyHome,
            socialSessionToken = sync.socialSessionToken,
            socialUserId = sync.socialUserId,
            socialUsername = sync.socialUsername,
            socialDisplayName = sync.socialDisplayName,
            socialAvatarColor = sync.socialAvatarColor,
            socialAvatarDoodle = sync.socialAvatarDoodle,
            socialAvatarUseDoodle = sync.socialAvatarUseDoodle,
            socialOnlineStatusEnabled = sync.socialOnlineStatusEnabled,
            socialShowNowPlaying = sync.socialShowNowPlaying,
            socialNotifyFriendOnline = sync.socialNotifyFriendOnline,
            socialNotifyFriendPlaying = sync.socialNotifyFriendPlaying,
            socialSuppressNotificationsInGame = sync.socialSuppressNotificationsInGame,
            discordRichPresenceEnabled = sync.discordRichPresenceEnabled,
            lastPlaySessionSync = sync.lastPlaySessionSync,
            lastStateValidation = sync.lastStateValidation,
            quayPassEnabled = sync.quayPassEnabled,
            quayPassAvatarSyncPending = sync.quayPassAvatarSyncPending,
            quayPassMessageSyncPending = sync.quayPassMessageSyncPending,
            quayPassGreeting = sync.quayPassGreeting,
            quayPassTicketBalance = sync.quayPassTicketBalance,
            mediaStoragePath = storage.mediaStoragePath,
            jellyfinServerUrl = jellyfin.serverUrl,
            jellyfinDeviceId = jellyfin.deviceId,
            jellyfinAccessToken = jellyfin.accessToken,
            jellyfinUserId = jellyfin.userId,
            jellyfinUserName = jellyfin.userName,
            mediaDownloadQuality = jellyfin.downloadQuality,
            mediaStreamingQuality = jellyfin.streamingQuality,
            mediaAudioLanguage = jellyfin.audioLanguage,
            mediaSubtitleMode = jellyfin.subtitleMode,
            mediaSubtitleLanguage = jellyfin.subtitleLanguage,
            mediaBurnInImageSubtitles = jellyfin.burnInImageSubtitles,
            mediaConfirmPlayerExit = jellyfin.confirmPlayerExit,
            shareMediaPresence = jellyfin.shareMediaPresence
        )
    }

    val userPreferences: Flow<UserPreferences> = preferencesSource

    val preferences: Flow<UserPreferences> = userPreferences

    private val hotPreferences: SharedFlow<UserPreferences> =
        preferencesSource.shareIn(shareScope, SharingStarted.Eagerly, replay = 1)

    /**
     * The most recent preferences without suspending, or null before the first DataStore read.
     * A seed for a screen's first frame; reads that must see a write just made use [userPreferences].
     */
    val latest: UserPreferences? get() = hotPreferences.replayCache.firstOrNull()

    // --- Display delegates ---

    suspend fun setThemeMode(mode: ThemeMode) = displayPrefs.setThemeMode(mode)
    suspend fun setCustomColors(primary: Int?, secondary: Int?, tertiary: Int?) = displayPrefs.setCustomColors(primary, secondary, tertiary)

    /**
     * Sets the accent colour, or the theme default when [color] is null. The secondary and
     * tertiary colours reset with it.
     */
    suspend fun setPrimaryColor(color: Int?) = displayPrefs.setCustomColors(color, null, null)
    suspend fun setSecondaryColor(color: Int?) = displayPrefs.setSecondaryColor(color)
    suspend fun setSurfaceTintBleed(bleed: Int) = displayPrefs.setSurfaceTintBleed(bleed)
    suspend fun setBackdropEnabled(enabled: Boolean) = displayPrefs.setBackdropEnabled(enabled)
    suspend fun setBackdropPreset(preset: BackdropPreset, edgeStyle: BackdropEdgeStyle, vertexIcons: BackdropVertexIcon) =
        displayPrefs.setBackdropPreset(preset, edgeStyle, vertexIcons)
    suspend fun setBackdropCellSize(sizeDp: Int) = displayPrefs.setBackdropCellSize(sizeDp)
    suspend fun setBackdropScatter(scatter: Int) = displayPrefs.setBackdropScatter(scatter)
    suspend fun setBackdropScaleJitter(jitter: Int) = displayPrefs.setBackdropScaleJitter(jitter)
    suspend fun setBackdropStrength(strength: Int) = displayPrefs.setBackdropStrength(strength)
    suspend fun setBackdropEdgeStyle(style: BackdropEdgeStyle) = displayPrefs.setBackdropEdgeStyle(style)
    suspend fun setBackdropVertexIcons(icons: BackdropVertexIcon) = displayPrefs.setBackdropVertexIcons(icons)
    suspend fun setBackdropSeed(seed: Long) = displayPrefs.setBackdropSeed(seed)
    suspend fun setBackdropMotion(motion: BackdropMotion) = displayPrefs.setBackdropMotion(motion)
    suspend fun setBackdropMotionSpeed(speed: Int) = displayPrefs.setBackdropMotionSpeed(speed)
    suspend fun setBackdropDriftAngle(angle: Float) = displayPrefs.setBackdropDriftAngle(angle)
    suspend fun setCustomFont(slot: FontSlot, path: String?, name: String?) = displayPrefs.setCustomFont(slot, path, name)
    suspend fun setFontScale(slot: FontSlot, scale: Int) = displayPrefs.setFontScale(slot, scale)
    suspend fun setGridDensity(density: GridDensity) = displayPrefs.setGridDensity(density)
    suspend fun setLibraryLayout(layout: LibraryLayout) = displayPrefs.setLibraryLayout(layout)

    suspend fun setLibraryBoxArt3d(enabled: Boolean) = displayPrefs.setLibraryBoxArt3d(enabled)

    suspend fun setLibraryDefaultSort(option: String, descending: Boolean) =
        displayPrefs.setLibraryDefaultSort(option, descending)

    suspend fun setSortInstalledFirst(enabled: Boolean) = displayPrefs.setSortInstalledFirst(enabled)

    suspend fun setSortFavoritesFirst(enabled: Boolean) = displayPrefs.setSortFavoritesFirst(enabled)

    suspend fun setLibraryDefaultSource(source: String) = displayPrefs.setLibraryDefaultSource(source)

    suspend fun setLibraryDefaultPlatformId(platformId: Long?) =
        displayPrefs.setLibraryDefaultPlatformId(platformId)

    suspend fun setLibraryDefaultRegions(regions: Set<String>) = displayPrefs.setLibraryDefaultRegions(regions)

    suspend fun setLibraryDefaultPlayers(bucket: com.nendo.argosy.domain.model.PlayerCountBucket?) =
        displayPrefs.setLibraryDefaultPlayers(bucket)

    suspend fun migrateLegacyDefaultPlatform(idForDisplayName: (String) -> Long?) =
        displayPrefs.migrateLegacyDefaultPlatform(idForDisplayName)
    suspend fun setUiScale(scale: Int) = displayPrefs.setUiScale(scale)
    suspend fun setGripReserveEnabled(enabled: Boolean) = displayPrefs.setGripReserveEnabled(enabled)
    suspend fun setGripReservePercent(percent: Int) = displayPrefs.setGripReservePercent(percent)
    suspend fun setBackgroundBlur(blur: Int) = displayPrefs.setBackgroundBlur(blur)
    suspend fun setBackgroundSaturation(saturation: Int) = displayPrefs.setBackgroundSaturation(saturation)
    suspend fun setBackgroundOpacity(opacity: Int) = displayPrefs.setBackgroundOpacity(opacity)
    suspend fun setUseGameBackground(use: Boolean) = displayPrefs.setUseGameBackground(use)
    suspend fun setCustomBackgroundPath(path: String?) = displayPrefs.setCustomBackgroundPath(path)
    suspend fun setHomeBackgroundMode(mode: HomeBackgroundMode) = displayPrefs.setHomeBackgroundMode(mode)
    suspend fun setHomeLayout(settings: com.nendo.argosy.domain.model.HomeLayoutSettings) =
        displayPrefs.setHomeLayout(settings)
    suspend fun setPresentationStyle(style: com.nendo.argosy.domain.model.PresentationStyle) =
        displayPrefs.setPresentationStyle(style)
    suspend fun setPresentationBackgroundBlur(blur: Int) = displayPrefs.setPresentationBackgroundBlur(blur)
    suspend fun setUseAccentColorFooter(use: Boolean) = displayPrefs.setUseAccentColorFooter(use)
    suspend fun setCompactFooter(enabled: Boolean) = displayPrefs.setCompactFooter(enabled)
    suspend fun setHideSystemNavigationBar(hide: Boolean) = displayPrefs.setHideSystemNavigationBar(hide)
    suspend fun setHideSystemStatusBar(hide: Boolean) = displayPrefs.setHideSystemStatusBar(hide)
    suspend fun setLockScreenArt(enabled: Boolean) = displayPrefs.setLockScreenArt(enabled)
    suspend fun setIconPack(packageName: String?) = displayPrefs.setIconPack(packageName)
    suspend fun setGripAutoControllers(
        controllers: com.nendo.argosy.domain.model.GripAutoControllers
    ) = displayPrefs.setGripAutoControllers(controllers)
    suspend fun setGripReserveMode(mode: GripReserveMode) = displayPrefs.setGripReserveMode(mode)
    suspend fun setBoxArtShape(shape: BoxArtShape) = displayPrefs.setBoxArtShape(shape)
    suspend fun setBoxArtCornerRadius(radius: BoxArtCornerRadius) = displayPrefs.setBoxArtCornerRadius(radius)
    suspend fun setBoxArtBorderThickness(thickness: BoxArtBorderThickness) = displayPrefs.setBoxArtBorderThickness(thickness)
    suspend fun setBoxArtBorderStyle(style: BoxArtBorderStyle) = displayPrefs.setBoxArtBorderStyle(style)
    suspend fun setGlassBorderTint(tint: GlassBorderTint) = displayPrefs.setGlassBorderTint(tint)
    suspend fun setBoxArtGlowStrength(strength: BoxArtGlowStrength) = displayPrefs.setBoxArtGlowStrength(strength)
    suspend fun setBoxArtOuterEffect(effect: BoxArtOuterEffect) = displayPrefs.setBoxArtOuterEffect(effect)
    suspend fun setBoxArtOuterEffectThickness(thickness: BoxArtOuterEffectThickness) = displayPrefs.setBoxArtOuterEffectThickness(thickness)
    suspend fun setGlowColorMode(mode: GlowColorMode) = displayPrefs.setGlowColorMode(mode)
    suspend fun setBoxArtInnerEffect(effect: BoxArtInnerEffect) = displayPrefs.setBoxArtInnerEffect(effect)
    suspend fun setBoxArtInnerEffectThickness(thickness: BoxArtInnerEffectThickness) = displayPrefs.setBoxArtInnerEffectThickness(thickness)
    suspend fun setGradientPreset(preset: GradientPreset) = displayPrefs.setGradientPreset(preset)
    suspend fun setGradientAdvancedMode(enabled: Boolean) = displayPrefs.setGradientAdvancedMode(enabled)
    suspend fun setSystemIconPosition(position: SystemIconPosition) = displayPrefs.setSystemIconPosition(position)
    suspend fun setSystemIconPadding(padding: SystemIconPadding) = displayPrefs.setSystemIconPadding(padding)
    suspend fun setPlatformIndicatorStyle(style: PlatformIndicatorStyle) = displayPrefs.setPlatformIndicatorStyle(style)
    suspend fun setPlatformIndicatorContent(content: PlatformIndicatorContent) = displayPrefs.setPlatformIndicatorContent(content)
    suspend fun setDefaultView(view: DefaultView) = displayPrefs.setDefaultView(view)
    suspend fun setVideoWallpaperEnabled(enabled: Boolean) = displayPrefs.setVideoWallpaperEnabled(enabled)
    suspend fun setVideoWallpaperDelaySeconds(seconds: Int) = displayPrefs.setVideoWallpaperDelaySeconds(seconds)
    suspend fun setVideoWallpaperMuted(muted: Boolean) = displayPrefs.setVideoWallpaperMuted(muted)
    suspend fun setAmbientLedEnabled(enabled: Boolean) = displayPrefs.setAmbientLedEnabled(enabled)
    suspend fun setAmbientLedBrightness(brightness: Int) = displayPrefs.setAmbientLedBrightness(brightness)
    suspend fun setAmbientLedAudioBrightness(enabled: Boolean) = displayPrefs.setAmbientLedAudioBrightness(enabled)
    suspend fun setAmbientLedAudioColors(enabled: Boolean) = displayPrefs.setAmbientLedAudioColors(enabled)
    suspend fun setAmbientLedColorMode(mode: AmbientLedColorMode) = displayPrefs.setAmbientLedColorMode(mode)
    suspend fun setAmbientLedCoverArtEnabled(enabled: Boolean) = displayPrefs.setAmbientLedCoverArtEnabled(enabled)
    suspend fun setAmbientLedCustomColor(enabled: Boolean) = displayPrefs.setAmbientLedCustomColor(enabled)
    suspend fun setAmbientLedCustomColorHue(hue: Int) = displayPrefs.setAmbientLedCustomColorHue(hue)
    suspend fun setAmbientLedTransitionMs(ms: Int) = displayPrefs.setAmbientLedTransitionMs(ms)
    suspend fun setAmbientLedScreenEnabled(enabled: Boolean) = displayPrefs.setAmbientLedScreenEnabled(enabled)
    suspend fun setAmbientLedAchievementFlash(enabled: Boolean) = displayPrefs.setAmbientLedAchievementFlash(enabled)
    suspend fun setScreenDimmerEnabled(enabled: Boolean) = displayPrefs.setScreenDimmerEnabled(enabled)
    suspend fun setScreenDimmerTimeoutMinutes(minutes: Int) = displayPrefs.setScreenDimmerTimeoutMinutes(minutes)
    suspend fun setScreenDimmerLevel(level: Int) = displayPrefs.setScreenDimmerLevel(level)
    suspend fun setDualScreenEnabled(enabled: Boolean) = displayPrefs.setDualScreenEnabled(enabled)
    suspend fun setDisplayRoleOverride(override: DisplayRoleOverride) = displayPrefs.setDisplayRoleOverride(override)
    suspend fun setGamesDisplayTarget(target: EmulatorDisplayTarget) = displayPrefs.setGamesDisplayTarget(target)
    suspend fun setAppsDisplayTarget(target: EmulatorDisplayTarget) = displayPrefs.setAppsDisplayTarget(target)
    suspend fun setScreenLayouts(layouts: ScreenLayouts) = displayPrefs.setScreenLayouts(layouts)
    suspend fun setDualScreenInputFocus(focus: DualScreenInputFocus) = displayPrefs.setDualScreenInputFocus(focus)
    suspend fun setInstalledOnlyHome(enabled: Boolean) = displayPrefs.setInstalledOnlyHome(enabled)

    suspend fun setShowStatusClock(show: Boolean) = displayPrefs.setShowStatusClock(show)

    suspend fun setShowStatusBattery(show: Boolean) = displayPrefs.setShowStatusBattery(show)

    suspend fun setShowStatusNetwork(show: Boolean) = displayPrefs.setShowStatusNetwork(show)

    // --- Sync delegates ---

    suspend fun setRommConfig(url: String?, username: String?) = syncPrefs.setRommConfig(url, username)

    suspend fun privateIgdbIds(): List<Long> = syncPrefs.privateIgdbIds()
    suspend fun setDownloadCategoryDefault(categoryKey: String, include: Boolean) =
        syncPrefs.setDownloadCategoryDefault(categoryKey, include)
    suspend fun setDownloadCategoryPlatformOverride(platformSlug: String, categoryKey: String, include: Boolean?) =
        syncPrefs.setDownloadCategoryPlatformOverride(platformSlug, categoryKey, include)
    suspend fun getGlobalDownloadDefaults(): Map<String, Boolean> =
        DownloadDefaults.FACTORY + syncPrefs.downloadCategoryDefaults.first()
    suspend fun getDownloadPlatformOverrides(platformSlug: String): Map<String, Boolean> =
        syncPrefs.downloadCategoryPlatformOverrides.first()[platformSlug] ?: emptyMap()
    suspend fun getEffectiveDownloadDefaults(platformSlug: String): Map<String, Boolean> =
        syncPrefs.getEffectiveDownloadDefaults(platformSlug)
    suspend fun setRomMCredentials(baseUrl: String, token: String, username: String? = null, userId: Long? = null) =
        syncPrefs.setRomMCredentials(baseUrl, token, username, userId)
    suspend fun setRomMAvatarPath(path: String?) = syncPrefs.setRomMAvatarPath(path)
    suspend fun clearRomMCredentials() = syncPrefs.clearRomMCredentials()
    suspend fun setRommDeviceId(deviceId: String, clientVersion: String) = syncPrefs.setRommDeviceId(deviceId, clientVersion)
    suspend fun clearRommDeviceId() = syncPrefs.clearRommDeviceId()
    suspend fun getRommSnapshotSupport(serverKey: String): Boolean? = syncPrefs.getRommSnapshotSupport(serverKey)
    suspend fun setRommSnapshotSupport(serverKey: String, supported: Boolean) =
        syncPrefs.setRommSnapshotSupport(serverKey, supported)
    suspend fun setRACredentials(username: String, token: String) = syncPrefs.setRACredentials(username, token)
    suspend fun clearRACredentials() = syncPrefs.clearRACredentials()
    suspend fun setRAWebApiKey(key: String?) = syncPrefs.setRAWebApiKey(key)
    suspend fun setRAProxy(enabled: Boolean, address: String) = syncPrefs.setRAProxy(enabled, address)
    suspend fun setLastRommSyncTime(time: Instant) = syncPrefs.setLastRommSyncTime(time)
    suspend fun setLastRommFullSyncTime(time: Instant) = syncPrefs.setLastRommFullSyncTime(time)
    suspend fun getSyncResumeGeneration(): Instant? = syncPrefs.getSyncResumeGeneration()
    suspend fun getSyncResumeCompletedPlatformIds(): Set<Long> = syncPrefs.getSyncResumeCompletedPlatformIds()
    suspend fun startSyncGeneration(time: Instant) = syncPrefs.startSyncGeneration(time)
    suspend fun addSyncResumeCompletedPlatform(platformId: Long) = syncPrefs.addSyncResumeCompletedPlatform(platformId)
    suspend fun clearSyncResume() = syncPrefs.clearSyncResume()
    suspend fun setLastFavoritesSyncTime(time: Instant) = syncPrefs.setLastFavoritesSyncTime(time)
    suspend fun setLastFavoritesCheckTime(time: Instant) = syncPrefs.setLastFavoritesCheckTime(time)
    suspend fun setSyncFilterRegions(regions: List<String>) = syncPrefs.setSyncFilterRegions(regions)
    suspend fun setSyncFilterRegionMode(mode: RegionFilterMode) = syncPrefs.setSyncFilterRegionMode(mode)
    suspend fun setRegionPriority(order: List<String>) = syncPrefs.setRegionPriority(order)
    suspend fun setSyncFilterExcludeBeta(exclude: Boolean) = syncPrefs.setSyncFilterExcludeBeta(exclude)
    suspend fun setSyncFilterExcludePrototype(exclude: Boolean) = syncPrefs.setSyncFilterExcludePrototype(exclude)
    suspend fun setSyncFilterExcludeDemo(exclude: Boolean) = syncPrefs.setSyncFilterExcludeDemo(exclude)
    suspend fun setSyncFilterExcludeHack(exclude: Boolean) = syncPrefs.setSyncFilterExcludeHack(exclude)
    suspend fun setSyncFilterExcludeUnofficial(exclude: Boolean) =
        syncPrefs.setSyncFilterExcludeUnofficial(exclude)
    suspend fun setSyncFilterDeleteOrphans(delete: Boolean) = syncPrefs.setSyncFilterDeleteOrphans(delete)
    suspend fun setSyncScreenshotsEnabled(enabled: Boolean) = syncPrefs.setSyncScreenshotsEnabled(enabled)
    suspend fun setUploadScreenshotsEnabled(enabled: Boolean) = syncPrefs.setUploadScreenshotsEnabled(enabled)
    suspend fun setBoxArtCacheEnabled(enabled: Boolean) = syncPrefs.setBoxArtCacheEnabled(enabled)
    suspend fun setSaveSyncEnabled(enabled: Boolean) = syncPrefs.setSaveSyncEnabled(enabled)
    suspend fun setSecureSaves(enabled: Boolean) = syncPrefs.setSecureSaves(enabled)
    suspend fun setStateCacheEnabled(enabled: Boolean) = syncPrefs.setStateCacheEnabled(enabled)
    suspend fun setSaveCacheLimit(limit: Int) = syncPrefs.setSaveCacheLimit(limit)
    suspend fun setSaveDebugLoggingEnabled(enabled: Boolean) = syncPrefs.setSaveDebugLoggingEnabled(enabled)
    suspend fun setImageCachePath(path: String?) = syncPrefs.setImageCachePath(path)
    fun saveWatcherEnabled(): Flow<Boolean> = syncPrefs.saveWatcherEnabled()
    suspend fun setSaveWatcherEnabled(enabled: Boolean) = syncPrefs.setSaveWatcherEnabled(enabled)

    // --- Social delegates ---

    suspend fun setSocialCredentials(
        sessionToken: String,
        userId: String,
        username: String,
        displayName: String?,
        avatarColor: String?
    ) = syncPrefs.setSocialCredentials(sessionToken, userId, username, displayName, avatarColor)
    suspend fun clearSocialCredentials() = syncPrefs.clearSocialCredentials()
    suspend fun setSocialAvatarUseDoodle(enabled: Boolean) = syncPrefs.setSocialAvatarUseDoodle(enabled)
    suspend fun setSocialOnlineStatusEnabled(enabled: Boolean) = syncPrefs.setSocialOnlineStatusEnabled(enabled)
    suspend fun setSocialShowNowPlaying(enabled: Boolean) = syncPrefs.setSocialShowNowPlaying(enabled)
    suspend fun setSocialNotifyFriendOnline(enabled: Boolean) = syncPrefs.setSocialNotifyFriendOnline(enabled)
    suspend fun setSocialNotifyFriendPlaying(enabled: Boolean) = syncPrefs.setSocialNotifyFriendPlaying(enabled)
    suspend fun setSocialSuppressNotificationsInGame(enabled: Boolean) = syncPrefs.setSocialSuppressNotificationsInGame(enabled)
    suspend fun setDiscordRichPresenceEnabled(enabled: Boolean) = syncPrefs.setDiscordRichPresenceEnabled(enabled)
    suspend fun setLastPlaySessionSyncTime(time: Instant) = syncPrefs.setLastPlaySessionSyncTime(time)
    suspend fun setQuayPassEnabled(enabled: Boolean) = syncPrefs.setQuayPassEnabled(enabled)
    suspend fun setQuayPassAvatarSyncPending(pending: Boolean) = syncPrefs.setQuayPassAvatarSyncPending(pending)
    suspend fun setQuayPassMessageSyncPending(pending: Boolean) = syncPrefs.setQuayPassMessageSyncPending(pending)
    suspend fun setQuayPassGreetingFromServer(greeting: String) = syncPrefs.setQuayPassGreetingFromServer(greeting)
    suspend fun setSocialAvatarFromServer(doodle: String?) = syncPrefs.setSocialAvatarFromServer(doodle)
    suspend fun setQuayPassGreeting(greeting: String) = syncPrefs.setQuayPassGreeting(greeting)
    suspend fun setQuayPassTicketBalance(balance: Int) = syncPrefs.setQuayPassTicketBalance(balance)

    // --- Controls delegates ---

    suspend fun setHapticEnabled(enabled: Boolean) = controlsPrefs.setHapticEnabled(enabled)
    suspend fun setHapticStrength(strength: Float) = controlsPrefs.setHapticStrength(strength)
    suspend fun adjustHapticStrength(delta: Float): Float = controlsPrefs.adjustHapticStrength(delta)
    suspend fun setSoundEnabled(enabled: Boolean) = controlsPrefs.setSoundEnabled(enabled)
    suspend fun setSoundVolume(volume: Int) = controlsPrefs.setSoundVolume(volume)
    suspend fun setSoundConfigs(configs: Map<SoundType, SoundConfig>) = controlsPrefs.setSoundConfigs(configs)
    suspend fun setSoundConfig(type: SoundType, config: SoundConfig?) = controlsPrefs.setSoundConfig(type, config)
    suspend fun setSwapAB(enabled: Boolean) = controlsPrefs.setSwapAB(enabled)
    suspend fun setSwapXY(enabled: Boolean) = controlsPrefs.setSwapXY(enabled)
    suspend fun setControllerLayout(layout: String) = controlsPrefs.setControllerLayout(layout)
    suspend fun setSwapStartSelect(enabled: Boolean) = controlsPrefs.setSwapStartSelect(enabled)
    suspend fun setSelectLCombo(value: String) = controlsPrefs.setSelectLCombo(value)
    suspend fun setSelectRCombo(value: String) = controlsPrefs.setSelectRCombo(value)
    suspend fun setAmbientAudioEnabled(enabled: Boolean) = controlsPrefs.setAmbientAudioEnabled(enabled)
    suspend fun setAmbientAudioVolume(volume: Int) = controlsPrefs.setAmbientAudioVolume(volume)
    suspend fun setAmbientAudioUri(uri: String?) = controlsPrefs.setAmbientAudioUri(uri)
    suspend fun setAmbientAudioShuffle(shuffle: Boolean) = controlsPrefs.setAmbientAudioShuffle(shuffle)
    suspend fun setGameDetailThemeEnabled(enabled: Boolean) = controlsPrefs.setGameDetailThemeEnabled(enabled)
    suspend fun setMenuWrapMode(mode: MenuWrapMode) = controlsPrefs.setMenuWrapMode(mode)
    suspend fun setSelectSwapMode(mode: SelectSwapMode) = controlsPrefs.setSelectSwapMode(mode)
    suspend fun setControllerInputMode(mode: ControllerInputMode) = controlsPrefs.setControllerInputMode(mode)
    suspend fun setOpenNavigationKey(keyCode: Int) = controlsPrefs.setOpenNavigationKey(keyCode)
    suspend fun setOpenQuickPanelKey(keyCode: Int) = controlsPrefs.setOpenQuickPanelKey(keyCode)
    suspend fun setNavRingRoutes(routes: List<String>?) = controlsPrefs.setNavRingRoutes(routes)
    suspend fun setQuickNavigation(enabled: Boolean) = controlsPrefs.setQuickNavigation(enabled)
    suspend fun setNavBarAutoHide(enabled: Boolean) = controlsPrefs.setNavBarAutoHide(enabled)


    // --- Storage delegates ---

    suspend fun setRomStoragePath(path: String) = storagePrefs.setRomStoragePath(path)
    suspend fun setMaxConcurrentDownloads(count: Int) = storagePrefs.setMaxConcurrentDownloads(count)
    suspend fun setAllowRemoteInstalls(enabled: Boolean) = storagePrefs.setAllowRemoteInstalls(enabled)
    suspend fun setAutoStartDownloads(enabled: Boolean) = storagePrefs.setAutoStartDownloads(enabled)
    suspend fun setInstantDownloadThresholdMb(value: Int) = storagePrefs.setInstantDownloadThresholdMb(value)
    suspend fun setStageDownloadsInternally(enabled: Boolean) = storagePrefs.setStageDownloadsInternally(enabled)

    suspend fun setFolderNameFromRom(enabled: Boolean) = storagePrefs.setFolderNameFromRom(enabled)
    suspend fun setCustomBiosPath(path: String?) = storagePrefs.setCustomBiosPath(path)
    suspend fun setWeeklyIntegrityCheckEnabled(enabled: Boolean) = storagePrefs.setWeeklyIntegrityCheckEnabled(enabled)
    suspend fun setLastIntegrityCheckTime(timeMs: Long) = storagePrefs.setLastIntegrityCheckTime(timeMs)
    suspend fun setSteamInstallVolume(volume: String?) = storagePrefs.setSteamInstallVolume(volume)
    suspend fun setMediaStoragePath(path: String?) = storagePrefs.setMediaStoragePath(path)

    suspend fun setJellyfinServerUrl(url: String?) = jellyfinPrefs.setServerUrl(url)
    suspend fun setJellyfinDeviceId(deviceId: String) = jellyfinPrefs.setDeviceId(deviceId)
    suspend fun setJellyfinCredentials(accessToken: String, userId: String, userName: String?) =
        jellyfinPrefs.setCredentials(accessToken, userId, userName)
    suspend fun clearJellyfinCredentials() = jellyfinPrefs.clearCredentials()
    suspend fun setMediaDownloadQuality(quality: MediaDownloadQuality) =
        jellyfinPrefs.setDownloadQuality(quality)
    suspend fun setMediaStreamingQuality(quality: MediaStreamingQuality) =
        jellyfinPrefs.setStreamingQuality(quality)
    suspend fun setMediaAudioLanguage(language: MediaAudioLanguage) =
        jellyfinPrefs.setAudioLanguage(language)
    suspend fun setMediaSubtitleMode(mode: MediaSubtitleMode) = jellyfinPrefs.setSubtitleMode(mode)
    suspend fun setMediaSubtitleLanguage(language: MediaSubtitleLanguage) =
        jellyfinPrefs.setSubtitleLanguage(language)
    suspend fun setMediaBurnInImageSubtitles(enabled: Boolean) =
        jellyfinPrefs.setBurnInImageSubtitles(enabled)
    suspend fun setMediaConfirmPlayerExit(enabled: Boolean) =
        jellyfinPrefs.setConfirmPlayerExit(enabled)
    suspend fun setShareMediaPresence(enabled: Boolean) = jellyfinPrefs.setShareMediaPresence(enabled)

    // --- App delegates ---

    suspend fun setFirstRunComplete() = appPrefs.setFirstRunComplete()
    suspend fun setBetaUpdatesEnabled(enabled: Boolean) = appPrefs.setBetaUpdatesEnabled(enabled)
    suspend fun setCloseEmulatorOnSessionEnd(enabled: Boolean) = appPrefs.setCloseEmulatorOnSessionEnd(enabled)
    suspend fun setHiddenApps(apps: Set<String>) = appPrefs.setHiddenApps(apps)
    suspend fun toggleDockPin(packageName: String) = appPrefs.toggleDockPin(packageName)
    suspend fun reorderDockPins(expected: List<String>, order: List<String>) =
        appPrefs.reorderDockPins(expected, order)
    suspend fun setVisibleSystemApps(apps: Set<String>) = appPrefs.setVisibleSystemApps(apps)
    suspend fun setAppOrder(order: List<String>) = appPrefs.setAppOrder(order)
    suspend fun setLastSeenVersion(version: String) = appPrefs.setLastSeenVersion(version)
    suspend fun addLibraryRecentSearch(query: String) = appPrefs.addLibraryRecentSearch(query)
    suspend fun setRecommendations(gameIds: List<Long>, timestamp: Instant) = appPrefs.setRecommendations(gameIds, timestamp)
    suspend fun clearRecommendations() = appPrefs.clearRecommendations()
    suspend fun setRecommendationPenalties(penalties: Map<Long, Float>, weekKey: String) = appPrefs.setRecommendationPenalties(penalties, weekKey)
    suspend fun setFileLoggingEnabled(enabled: Boolean) = appPrefs.setFileLoggingEnabled(enabled)
    suspend fun setFileLoggingPath(path: String?) = appPrefs.setFileLoggingPath(path)
    suspend fun setFileLogLevel(level: LogLevel) = appPrefs.setFileLogLevel(level)
    suspend fun setAppDisplayTarget(packageName: String, screenKey: String?) =
        appPrefs.setAppDisplayTarget(packageName, screenKey)
    suspend fun setAppLanguage(tag: String) = appPrefs.setAppLanguage(tag)
    suspend fun setPreLaunchRecapsEnabled(enabled: Boolean) = appPrefs.setPreLaunchRecapsEnabled(enabled)
    suspend fun setMonthlyRecapEnabled(enabled: Boolean) = appPrefs.setMonthlyRecapEnabled(enabled)
    suspend fun setLastMonthlyRecapShown(month: java.time.YearMonth) = appPrefs.setLastMonthlyRecapShown(month)

    suspend fun setPreLaunchRecapBaseline(at: java.time.Instant) = appPrefs.setPreLaunchRecapBaseline(at)

    // --- Builtin emulator delegates ---

    suspend fun setBuiltinShader(shader: String) = builtinPrefs.setBuiltinShader(shader)
    suspend fun setBuiltinShaderChain(chainJson: String) = builtinPrefs.setBuiltinShaderChain(chainJson)
    suspend fun setBuiltinFilter(filter: String) = builtinPrefs.setBuiltinFilter(filter)
    suspend fun setBuiltinLibretroEnabled(enabled: Boolean) = builtinPrefs.setBuiltinLibretroEnabled(enabled)
    suspend fun setBuiltinAspectRatio(aspectRatio: String) = builtinPrefs.setBuiltinAspectRatio(aspectRatio)
    suspend fun setBuiltinPortraitPosition(value: String) = builtinPrefs.setBuiltinPortraitPosition(value)
    suspend fun setBuiltinSkipDuplicateFrames(enabled: Boolean) = builtinPrefs.setBuiltinSkipDuplicateFrames(enabled)
    suspend fun setBuiltinLowLatencyAudio(enabled: Boolean) = builtinPrefs.setBuiltinLowLatencyAudio(enabled)
    suspend fun setBuiltinForceSoftwareTiming(enabled: Boolean) = builtinPrefs.setBuiltinForceSoftwareTiming(enabled)
    suspend fun setBuiltinRumbleEnabled(enabled: Boolean) = builtinPrefs.setBuiltinRumbleEnabled(enabled)
    suspend fun setBuiltinBlackFrameInsertion(enabled: Boolean) = builtinPrefs.setBuiltinBlackFrameInsertion(enabled)
    suspend fun setBuiltinLimitHotkeysToPlayer1(enabled: Boolean) = builtinPrefs.setBuiltinLimitHotkeysToPlayer1(enabled)
    suspend fun setBuiltinAnalogAsDpad(enabled: Boolean) = builtinPrefs.setBuiltinAnalogAsDpad(enabled)
    suspend fun setBuiltinDpadAsAnalog(enabled: Boolean) = builtinPrefs.setBuiltinDpadAsAnalog(enabled)
    suspend fun setBuiltinCoreForPlatform(platformSlug: String, coreId: String) = builtinPrefs.setBuiltinCoreForPlatform(platformSlug, coreId)
    suspend fun setBuiltinFastForwardEnabled(enabled: Boolean) = builtinPrefs.setBuiltinFastForwardEnabled(enabled)
    suspend fun setBuiltinFastForwardSpeed(speed: Int) = builtinPrefs.setBuiltinFastForwardSpeed(speed)
    suspend fun setBuiltinFastForwardMode(mode: com.nendo.argosy.data.local.entity.FastForwardMode) = builtinPrefs.setBuiltinFastForwardMode(mode)
    suspend fun setBuiltinFastForwardPreservePitch(enabled: Boolean) = builtinPrefs.setBuiltinFastForwardPreservePitch(enabled)
    suspend fun setBuiltinRotation(rotation: Int) = builtinPrefs.setBuiltinRotation(rotation)
    suspend fun setBuiltinOverscanCrop(crop: Int) = builtinPrefs.setBuiltinOverscanCrop(crop)
    suspend fun setBuiltinRewindEnabled(enabled: Boolean) = builtinPrefs.setBuiltinRewindEnabled(enabled)
    suspend fun setBuiltinRewindSpeed(speed: Int) = builtinPrefs.setBuiltinRewindSpeed(speed)
    suspend fun setBuiltinRewindBufferDuration(duration: Int) = builtinPrefs.setBuiltinRewindBufferDuration(duration)
    suspend fun setBuiltinFramesEnabled(enabled: Boolean) = builtinPrefs.setBuiltinFramesEnabled(enabled)
    suspend fun setBuiltinAutoSaveState(enabled: Boolean) = builtinPrefs.setBuiltinAutoSaveState(enabled)
    suspend fun setBuiltinAutoRestoreState(enabled: Boolean) = builtinPrefs.setBuiltinAutoRestoreState(enabled)
    suspend fun setBuiltinAutoRestoreStateMode(mode: String) = builtinPrefs.setBuiltinAutoRestoreStateMode(mode)
    suspend fun setBuiltinPreferNewerServerSave(enabled: Boolean) = builtinPrefs.setBuiltinPreferNewerServerSave(enabled)
    suspend fun setBuiltinCustomSavePath(path: String?) = builtinPrefs.setBuiltinCustomSavePath(path)
    suspend fun setBuiltinCustomStatePath(path: String?) = builtinPrefs.setBuiltinCustomStatePath(path)
    suspend fun setBuiltinMigrationComplete() = builtinPrefs.setBuiltinMigrationComplete()
    suspend fun setArchitectureOverride(abi: String?) = builtinPrefs.setArchitectureOverride(abi)
    suspend fun setTouchControlsShowWhenNoGamepad(enabled: Boolean) = builtinPrefs.setTouchControlsShowWhenNoGamepad(enabled)
    suspend fun setTouchControlsOpacityLandscape(opacity: Float) = builtinPrefs.setTouchControlsOpacityLandscape(opacity)
    suspend fun setTouchControlsOpacityPortrait(opacity: Float) = builtinPrefs.setTouchControlsOpacityPortrait(opacity)
    suspend fun setTouchControlsSizeScale(scale: Float) = builtinPrefs.setTouchControlsSizeScale(scale)
    suspend fun setTouchControlsHaptic(enabled: Boolean) = builtinPrefs.setTouchControlsHaptic(enabled)
    suspend fun setTouchControlsFadeOnIdle(enabled: Boolean) = builtinPrefs.setTouchControlsFadeOnIdle(enabled)
    suspend fun setTouchControlsSwapHanded(enabled: Boolean) = builtinPrefs.setTouchControlsSwapHanded(enabled)
    suspend fun setTouchControlsLockOrientation(enabled: Boolean) = builtinPrefs.setTouchControlsLockOrientation(enabled)
    suspend fun setTouchControlsMirror180(enabled: Boolean) = builtinPrefs.setTouchControlsMirror180(enabled)
    suspend fun setTouchControlsAllowLongPressEdit(enabled: Boolean) = builtinPrefs.setTouchControlsAllowLongPressEdit(enabled)
    suspend fun setTouchControlsColouredFaceButtons(enabled: Boolean) = builtinPrefs.setTouchControlsColouredFaceButtons(enabled)
    suspend fun setTouchControlsGenesis6Button(enabled: Boolean) = builtinPrefs.setTouchControlsGenesis6Button(enabled)

    suspend fun setHudEnabled(enabled: Boolean) = builtinPrefs.setHudEnabled(enabled)

    suspend fun setHudCorner(corner: String) = builtinPrefs.setHudCorner(corner)

    suspend fun setHudShowBattery(enabled: Boolean) = builtinPrefs.setHudShowBattery(enabled)

    suspend fun setHudShowClock(enabled: Boolean) = builtinPrefs.setHudShowClock(enabled)

    suspend fun setHudShowPlaytime(enabled: Boolean) = builtinPrefs.setHudShowPlaytime(enabled)

    suspend fun setHudShowFps(enabled: Boolean) = builtinPrefs.setHudShowFps(enabled)

    suspend fun setHudShowLastSave(enabled: Boolean) = builtinPrefs.setHudShowLastSave(enabled)
    fun getArchitectureOverride(): Flow<String?> = builtinPrefs.getArchitectureOverride()
    fun getBuiltinEmulatorSettings(): Flow<BuiltinEmulatorSettings> = builtinPrefs.getBuiltinEmulatorSettings()
    fun getBuiltinCoreSelections(): Flow<Map<String, String>> = builtinPrefs.getBuiltinCoreSelections()
    suspend fun removeBuiltinCoreSelectionsByCore(coreId: String) = builtinPrefs.removeBuiltinCoreSelectionsByCore(coreId)
    fun isBuiltinMigrationComplete(): Flow<Boolean> = builtinPrefs.isBuiltinMigrationComplete()
    fun isPs2StandalonePinDone(): Flow<Boolean> = builtinPrefs.isPs2StandalonePinDone()
    suspend fun setPs2StandalonePinDone() = builtinPrefs.setPs2StandalonePinDone()

    // --- Session delegates ---

    val activeSessionFlow: Flow<PersistedSession?> = sessionPrefs.activeSessionFlow

    suspend fun persistActiveSession(
        gameId: Long,
        emulatorPackage: String,
        startTime: Instant,
        coreName: String?,
        isHardcore: Boolean,
        channelName: String? = null,
        variantFileId: Long? = null,
        origin: LaunchOrigin = LaunchOrigin.INTERNAL,
        isNetplayGuest: Boolean = false
    ) = sessionPrefs.persistActiveSession(gameId, emulatorPackage, startTime, coreName, isHardcore, channelName, variantFileId, origin, isNetplayGuest)

    suspend fun clearActiveSession() = sessionPrefs.clearActiveSession()

    suspend fun getPersistedSession(): PersistedSession? = sessionPrefs.getPersistedSession()

    suspend fun keepSessionForRetry(maxAttempts: Int): Boolean = sessionPrefs.keepSessionForRetry(maxAttempts)
}
