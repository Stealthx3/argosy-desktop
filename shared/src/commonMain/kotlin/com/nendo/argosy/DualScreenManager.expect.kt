package com.nendo.argosy

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.data.preferences.DisplayRoleOverride
import com.nendo.argosy.ui.input.InputSignature
import com.nendo.argosy.core.game.AchievementUi
import com.nendo.argosy.ui.dualscreen.CompanionDetail
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

expect class DualScreenManager {
    companion object {
        val ACTION_WIZARD_STATE: String

        val OVERLAY_MENU: String

        val OVERLAY_QUICK_MENU: String

        val OVERLAY_QUICK_SETTINGS: String

        val MEDIA_DIM_LOG_TAG: String
    }

    fun startGame(request: com.nendo.argosy.ui.screens.common.GameLaunchRequest): Result<Unit>

    val dockReorder: StateFlow<com.nendo.argosy.ui.components.DockReorderSession?>

    val homeAppsFlow: StateFlow<List<String>>

    val isRolesSwapped: StateFlow<Boolean>

    val hasPresentationScreen: StateFlow<Boolean>

    val controlHints: StateFlow<List<com.nendo.argosy.ui.dualscreen.CompanionHint>>

    fun publishControlHints(hints: List<com.nendo.argosy.ui.dualscreen.CompanionHint>)

    val mutedNotificationKeys: StateFlow<Set<String>>

    fun setMutedNotificationKeys(keys: Set<String>)

    val unconfiguredScreenSet: StateFlow<String?>

    fun clearUnconfiguredScreenSet()

    val screenNumbers: StateFlow<Map<Int, Int>>

    fun showScreenNumbers(size: com.nendo.argosy.hardware.DisplayBadgeSize = com.nendo.argosy.hardware.DisplayBadgeSize.LARGE)

    fun hideScreenNumbers()

    val focusPickerOpen: StateFlow<Boolean>

    val focusPickerIndex: StateFlow<Int>

    fun openFocusPicker()

    fun closeFocusPicker()

    fun moveFocusPicker(delta: Int)

    fun confirmFocusPicker()

    fun applyStoredScreenLayout(promptWhenUnknown: Boolean = false)

    fun setPrimaryDisplayId(displayId: Int)

    fun rederiveRolesFromLayout()

    val localeChangeToken: StateFlow<Int>

    fun notifyLocaleChanged()

    fun interactiveDisplayId(): Int?

    fun showcaseDisplayId(): Int?

    fun roleDisplayAreas(): Pair<Long?, Long?>

    val isDualScreenDevice: StateFlow<Boolean>

    fun setDualScreenDevice(value: Boolean)

    fun applyDualScreenEnabled(enabled: Boolean, isToggle: Boolean)

    fun reprobeSecondaryDisplay()

    fun companionTargetVanished(): Boolean

    fun fallbackToSingleScreen(persistent: Boolean)

    fun onDesktopModeChanged()

    fun hasLiveSession(): Boolean

    fun teardownCompanion()

    val userActive: StateFlow<Boolean>

    val lastUserActivityAtMs: StateFlow<Long>

    fun notifyUserActivity(source: String)

    val mediaDimBrightness: StateFlow<Float?>

    val mediaDimCoverAlpha: StateFlow<Float>

    fun claimInput(signature: InputSignature): Boolean

    val sessionControls: StateFlow<com.nendo.argosy.ui.dualscreen.dashboard.SessionControls>

    fun updateSessionControls(controls: com.nendo.argosy.ui.dualscreen.dashboard.SessionControls)

    fun openDashboardDocument(document: com.nendo.argosy.ui.screens.gamedetail.GameDocument)

    fun updateCompanionSaveDirty(isDirty: Boolean)

    var sessionRefocus: (() -> Unit)?

    fun holdsAppScreen(displayId: Int): Boolean

    fun focusableDisplays(): List<com.nendo.argosy.util.AttachedScreen>

    fun focusDisplay(displayId: Int)

    fun releaseStaleAppScreens()

    fun notifyLibraryRefresh()

    var onEmulatorDispatcherChanged: (() -> Unit)?

    val emulatorDisplay: StateFlow<Int?>

    var emulatorDisplayId: Int?

    val isExternalDisplay: Boolean

    var onRoleSwapped: ((Boolean) -> Unit)?

    fun setCompanionDetail(owner: com.nendo.argosy.ui.dualscreen.SlotOwner, detail: CompanionDetail?)

    val isCompanionActive: StateFlow<Boolean>

    val companionHoldsPrimary: StateFlow<Boolean>

    val presentationShowsHints: StateFlow<Boolean>

    val presentationStyle: StateFlow<com.nendo.argosy.domain.model.PresentationStyle>

    val presentationBackgroundBlur: StateFlow<Int>

    val selectSwapMode: StateFlow<com.nendo.argosy.data.preferences.SelectSwapMode>

    fun presentSlot(owner: com.nendo.argosy.ui.dualscreen.SlotOwner, slot: com.nendo.argosy.ui.dualscreen.PresentationSlot)

    fun releaseSlot(owner: com.nendo.argosy.ui.dualscreen.SlotOwner)

    val primaryOnHome: StateFlow<Boolean>

    fun setPrimaryOnHome(onHome: Boolean)

    val primaryRoute: StateFlow<String?>

    fun setPrimaryRoute(route: String?)

    val mediaPlayback: StateFlow<com.nendo.argosy.data.media.ActiveMediaPlayback?>

    val mediaSignedIn: StateFlow<Boolean>

    val companionMediaVisible: StateFlow<Boolean>

    val mediaPlayerDisplay: StateFlow<Int?>

    var mediaPlayerDisplayId: Int?

    val mediaPlayerControlsLocked: StateFlow<Boolean>

    fun setMediaPlayerControlsLocked(locked: Boolean)

    fun toggleCompanionMediaView()

    fun setCompanionMediaVisible(visible: Boolean)

    val mediaInfoRequest: StateFlow<String?>

    fun requestMediaInfo(itemId: String)

    fun clearMediaInfoRequest()

    fun mediaPlayerRelocationDisplayId(): Int?

    fun playMediaItem(itemId: String, startOver: Boolean = false)

    fun directMediaPlayerFocus(displayId: Int)

    val companionAchievements: StateFlow<List<AchievementUi>>

    val pendingOverlayEvent: StateFlow<String?>

    val keyboardToggleEvent: StateFlow<Long>

    var onOverlayFocusChanged: ((Boolean) -> Unit)?

    val swappedIsGameActive: StateFlow<Boolean>

    val swappedCompanionState: StateFlow<com.nendo.argosy.hardware.CompanionInGameState>

    fun primaryShowsDashboard(primaryDisplayId: Int?, gameDisplayId: Int? = emulatorDisplayId): Boolean

    val presentationSlot: StateFlow<com.nendo.argosy.ui.dualscreen.PresentationSlot>

    val isMovingGame: Boolean

    fun onGameMovedToDisplay(displayId: Int)

    val liveSwapAvailable: StateFlow<Boolean>

    suspend fun emulatorLeftScreen(context: Context, launcherStillInFront: () -> Boolean): Boolean

    fun endSessionAfterEmulatorLeft()

    val homeAppsList: List<String>

    fun beginDockReorder(packageName: String, controller: Boolean): Long?

    fun targetDockReorder(token: Long, target: Int)

    fun moveDockReorder(token: Long, delta: Int)

    fun cancelDockReorder(token: Long)

    fun saveDockReorder(token: Long)

    suspend fun installedAppLabels(): List<Pair<String, String>>

    fun appLabel(packageName: String): String

    suspend fun isAppPinned(packageName: String): Boolean

    suspend fun isAppHidden(packageName: String): Boolean

    fun appMenuRowsFor(screens: List<com.nendo.argosy.util.AttachedScreen>): List<com.nendo.argosy.ui.components.AppMenuRow>

    fun runAppMenuRow(packageName: String, row: com.nendo.argosy.ui.components.AppMenuRow)

    fun clearPendingOverlay()

    fun onCompanionResumed()

    fun onCompanionStopped(displayId: Int, finishing: Boolean, recreating: Boolean)

    fun onOpenOverlayFromCompanion(eventName: String)

    fun toggleUpperKeyboard()

    fun onRefocusUpper()

    fun onSessionChanged(gameId: Long, isHardcore: Boolean = false, channelName: String? = null)

    fun onSessionHardcoreChanged(isHardcore: Boolean, channelName: String?)

    fun onDownloadCompleted(gameId: Long)

    fun resyncCompanionState()

    fun broadcastForegroundState(isForeground: Boolean)

    val isWizardActive: StateFlow<Boolean>

    fun recordWizardState(isActive: Boolean)

    fun swapRoles()

    fun applyDisplayRoleOverride(override: DisplayRoleOverride)

    fun broadcastOpenOverlay(eventName: String)

    fun restoreEmulatorFocus()

    fun setEmulatorDisplay(displayId: Int?)

    fun assignEmulatorDisplayForSessionStart()

    fun startStartupGuard()

    fun stopStartupGuard()

    fun ensureCompanionLaunched(allowDuringSession: Boolean = false)

    fun ensureAppScreenLaunched()

    fun launchCompanionApp(packageName: String, pinnedScreenKey: String? = null)

    fun broadcastSessionCleared()

    fun registerReceivers()

    fun unregisterReceivers()

    fun refocusSession()
}
