package com.nendo.argosy

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.core.game.AchievementUi
import com.nendo.argosy.data.preferences.DisplayRoleOverride
import com.nendo.argosy.ui.input.InputSignature
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import com.nendo.argosy.ui.dualscreen.CompanionDetail
import com.nendo.argosy.platform.PlatformUri as Uri

actual class DualScreenManager @Inject constructor() {
    actual companion object {
        actual const val ACTION_WIZARD_STATE: String = "com.nendo.argosy.WIZARD_STATE"

        actual const val OVERLAY_MENU: String = "com.nendo.argosy.OVERLAY_MENU"

        actual const val OVERLAY_QUICK_MENU: String = "com.nendo.argosy.OVERLAY_QUICK_MENU"

        actual const val OVERLAY_QUICK_SETTINGS: String = "com.nendo.argosy.OVERLAY_QUICK_SETTINGS"

        actual const val MEDIA_DIM_LOG_TAG: String = "MediaDimRamp"
    }

    actual val dockReorder: StateFlow<com.nendo.argosy.ui.components.DockReorderSession?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual val homeAppsFlow: StateFlow<List<String>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())

    actual val isRolesSwapped: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual val hasPresentationScreen: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual val controlHints: StateFlow<List<com.nendo.argosy.ui.dualscreen.CompanionHint>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())

    actual fun publishControlHints(hints: List<com.nendo.argosy.ui.dualscreen.CompanionHint>) {
    }

    actual val mutedNotificationKeys: StateFlow<Set<String>> = kotlinx.coroutines.flow.MutableStateFlow(emptySet())

    actual fun setMutedNotificationKeys(keys: Set<String>) {
    }

    actual val unconfiguredScreenSet: StateFlow<String?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual fun clearUnconfiguredScreenSet() {
    }

    actual val screenNumbers: StateFlow<Map<Int, Int>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap())

    actual fun showScreenNumbers(size: com.nendo.argosy.hardware.DisplayBadgeSize) {
    }

    actual fun hideScreenNumbers() {
    }

    actual val focusPickerOpen: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual val focusPickerIndex: StateFlow<Int> = kotlinx.coroutines.flow.MutableStateFlow(0)

    actual fun openFocusPicker() {
    }

    actual fun closeFocusPicker() {
    }

    actual fun moveFocusPicker(delta: Int) {
    }

    actual fun confirmFocusPicker() {
    }

    actual fun applyStoredScreenLayout(promptWhenUnknown: Boolean) {
    }

    actual fun setPrimaryDisplayId(displayId: Int) {
    }

    actual fun rederiveRolesFromLayout() {
    }

    actual val localeChangeToken: StateFlow<Int> = kotlinx.coroutines.flow.MutableStateFlow(0)

    actual fun notifyLocaleChanged() {
    }

    actual fun interactiveDisplayId(): Int? = null

    actual fun showcaseDisplayId(): Int? = null

    actual fun roleDisplayAreas(): Pair<Long?, Long?> = TODO("desktop: roleDisplayAreas")

    actual val isDualScreenDevice: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual fun setDualScreenDevice(value: Boolean) {
    }

    actual fun applyDualScreenEnabled(enabled: Boolean, isToggle: Boolean) {
    }

    actual fun reprobeSecondaryDisplay() {
    }

    actual fun companionTargetVanished(): Boolean = false

    actual fun fallbackToSingleScreen(persistent: Boolean) {
    }

    actual fun onDesktopModeChanged() {
    }

    actual fun hasLiveSession(): Boolean = false

    actual fun teardownCompanion() {
    }

    actual val userActive: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual val lastUserActivityAtMs: StateFlow<Long> = kotlinx.coroutines.flow.MutableStateFlow(0L)

    actual fun notifyUserActivity(source: String) {
    }

    actual val mediaDimBrightness: StateFlow<Float?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual val mediaDimCoverAlpha: StateFlow<Float> = kotlinx.coroutines.flow.MutableStateFlow(0f)

    actual fun claimInput(signature: InputSignature): Boolean = false

    actual val sessionControls: StateFlow<com.nendo.argosy.ui.dualscreen.dashboard.SessionControls> = TODO("desktop: sessionControls")

    actual fun updateSessionControls(controls: com.nendo.argosy.ui.dualscreen.dashboard.SessionControls) {
    }

    actual fun updateCompanionSaveDirty(isDirty: Boolean) {
    }

    actual var sessionRefocus: (() -> Unit)? = null

    actual fun holdsAppScreen(displayId: Int): Boolean = false

    actual fun focusableDisplays(): List<com.nendo.argosy.util.AttachedScreen> = emptyList()

    actual fun focusDisplay(displayId: Int) {
    }

    actual fun releaseStaleAppScreens() {
    }

    actual fun notifyLibraryRefresh() {
    }

    actual var onEmulatorDispatcherChanged: (() -> Unit)? = null

    actual val emulatorDisplay: StateFlow<Int?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual var emulatorDisplayId: Int? = null

    actual val isExternalDisplay: Boolean = false

    actual var onRoleSwapped: ((Boolean) -> Unit)? = null

    actual val isCompanionActive: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual val companionHoldsPrimary: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual val presentationShowsHints: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual val presentationStyle: StateFlow<com.nendo.argosy.domain.model.PresentationStyle> = TODO("desktop: presentationStyle")

    actual val presentationBackgroundBlur: StateFlow<Int> = kotlinx.coroutines.flow.MutableStateFlow(0)

    actual val selectSwapMode: StateFlow<com.nendo.argosy.data.preferences.SelectSwapMode> = TODO("desktop: selectSwapMode")

    actual val primaryOnHome: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual fun setPrimaryOnHome(onHome: Boolean) {
    }

    actual val primaryRoute: StateFlow<String?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual fun setPrimaryRoute(route: String?) {
    }

    actual val mediaPlayback: StateFlow<com.nendo.argosy.data.media.ActiveMediaPlayback?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual val mediaSignedIn: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual val companionMediaVisible: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual val mediaPlayerDisplay: StateFlow<Int?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual var mediaPlayerDisplayId: Int? = null

    actual val mediaPlayerControlsLocked: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual fun setMediaPlayerControlsLocked(locked: Boolean) {
    }

    actual fun toggleCompanionMediaView() {
    }

    actual fun setCompanionMediaVisible(visible: Boolean) {
    }

    actual val mediaInfoRequest: StateFlow<String?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual fun requestMediaInfo(itemId: String) {
    }

    actual fun clearMediaInfoRequest() {
    }

    actual fun mediaPlayerRelocationDisplayId(): Int? = null

    actual fun playMediaItem(itemId: String, startOver: Boolean) {
    }

    actual fun directMediaPlayerFocus(displayId: Int) {
    }

    actual val companionAchievements: StateFlow<List<AchievementUi>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())

    actual val pendingOverlayEvent: StateFlow<String?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual val keyboardToggleEvent: StateFlow<Long> = kotlinx.coroutines.flow.MutableStateFlow(0L)

    actual var onOverlayFocusChanged: ((Boolean) -> Unit)? = null

    actual val swappedIsGameActive: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual fun primaryShowsDashboard(primaryDisplayId: Int?, gameDisplayId: Int?): Boolean = false

    actual val isMovingGame: Boolean = false

    actual fun onGameMovedToDisplay(displayId: Int) {
    }

    actual val liveSwapAvailable: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual suspend fun emulatorLeftScreen(context: Context, launcherStillInFront: () -> Boolean): Boolean = false

    actual fun endSessionAfterEmulatorLeft() {
    }

    actual val homeAppsList: List<String> = emptyList()

    actual fun beginDockReorder(packageName: String, controller: Boolean): Long? = null

    actual suspend fun installedAppLabels(): List<Pair<String, String>> = emptyList()

    actual fun appLabel(packageName: String): String = ""

    actual suspend fun isAppPinned(packageName: String): Boolean = false

    actual suspend fun isAppHidden(packageName: String): Boolean = false

    actual fun appMenuRowsFor(screens: List<com.nendo.argosy.util.AttachedScreen>): List<com.nendo.argosy.ui.components.AppMenuRow> = emptyList()

    actual fun runAppMenuRow(packageName: String, row: com.nendo.argosy.ui.components.AppMenuRow) {
    }

    actual fun clearPendingOverlay() {
    }

    actual fun onCompanionResumed() {
    }

    actual fun onCompanionStopped(displayId: Int, finishing: Boolean, recreating: Boolean) {
    }

    actual fun onOpenOverlayFromCompanion(eventName: String) {
    }

    actual fun toggleUpperKeyboard() {
    }

    actual fun onRefocusUpper() {
    }

    actual fun onSessionChanged(gameId: Long, isHardcore: Boolean, channelName: String?) {
    }

    actual fun onSessionHardcoreChanged(isHardcore: Boolean, channelName: String?) {
    }

    actual fun onDownloadCompleted(gameId: Long) {
    }

    actual fun resyncCompanionState() {
    }

    actual fun broadcastForegroundState(isForeground: Boolean) {
    }

    actual val isWizardActive: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual fun recordWizardState(isActive: Boolean) {
    }

    actual fun swapRoles() {
    }

    actual fun applyDisplayRoleOverride(override: DisplayRoleOverride) {
    }

    actual fun broadcastOpenOverlay(eventName: String) {
    }

    actual fun restoreEmulatorFocus() {
    }

    actual fun setEmulatorDisplay(displayId: Int?) {
    }

    actual fun assignEmulatorDisplayForSessionStart() {
    }

    actual fun startStartupGuard() {
    }

    actual fun stopStartupGuard() {
    }

    actual fun ensureCompanionLaunched(allowDuringSession: Boolean) {
    }

    actual fun ensureAppScreenLaunched() {
    }

    actual fun launchCompanionApp(packageName: String, pinnedScreenKey: String?) {
    }

    actual fun broadcastSessionCleared() {
    }

    actual fun registerReceivers() {
    }

    actual fun unregisterReceivers() {
    }

    actual fun refocusSession() {
    }

    actual fun targetDockReorder(token: Long, target: Int) {
    }

    actual fun moveDockReorder(token: Long, delta: Int) {
    }

    actual fun cancelDockReorder(token: Long) {
    }

    actual fun saveDockReorder(token: Long) {
    }

    actual val swappedCompanionState: StateFlow<com.nendo.argosy.hardware.CompanionInGameState> = TODO("desktop: swappedCompanionState")

    actual fun startGame(request: com.nendo.argosy.ui.screens.common.GameLaunchRequest): Result<Unit> = kotlin.Result.failure(UnsupportedOperationException("not available on desktop"))

    actual fun openDashboardDocument(document: com.nendo.argosy.ui.screens.gamedetail.GameDocument) {
    }

    actual fun setCompanionDetail(owner: com.nendo.argosy.ui.dualscreen.SlotOwner, detail: CompanionDetail?) {
    }

    actual fun presentSlot(owner: com.nendo.argosy.ui.dualscreen.SlotOwner, slot: com.nendo.argosy.ui.dualscreen.PresentationSlot) {
    }

    actual fun releaseSlot(owner: com.nendo.argosy.ui.dualscreen.SlotOwner) {
    }

    actual val presentationSlot: StateFlow<com.nendo.argosy.ui.dualscreen.PresentationSlot> = TODO("desktop: presentationSlot")

    actual val pendingDeepLink: StateFlow<com.nendo.argosy.platform.PlatformUri?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual fun publishDeepLink(uri: com.nendo.argosy.platform.PlatformUri) {
    }

    actual fun consumeDeepLink(uri: com.nendo.argosy.platform.PlatformUri): Boolean = false

    actual class ResolvedScreenLayout(
        actual val attached: List<com.nendo.argosy.util.AttachedScreen>,
        actual val setKey: String,
        actual val stored: com.nendo.argosy.domain.model.ScreenLayouts,
        actual val known: com.nendo.argosy.domain.model.ScreenLayout?,
        actual val layout: com.nendo.argosy.domain.model.ScreenLayout
    )

    actual suspend fun resolveScreenLayout(): ResolvedScreenLayout? = null

    actual fun registerCompanionHost(displayId: Int, host: CompanionHost) {
    }

    actual fun unregisterCompanionHost(displayId: Int, host: CompanionHost) {
    }

    actual val controlCompanion: CompanionHost? = null

    actual var isOverlayFocused: Boolean = false
}
