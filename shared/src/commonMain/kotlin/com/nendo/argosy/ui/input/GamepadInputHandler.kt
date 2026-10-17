package com.nendo.argosy.ui.input

import com.nendo.argosy.platform.PlatformInputDevice as InputDevice
import com.nendo.argosy.platform.controllerNumber
import com.nendo.argosy.platform.descriptor
import com.nendo.argosy.platform.id
import com.nendo.argosy.platform.isEnabled
import com.nendo.argosy.platform.isExternal
import com.nendo.argosy.platform.isVirtual
import com.nendo.argosy.platform.name
import com.nendo.argosy.platform.productId
import com.nendo.argosy.platform.sources
import com.nendo.argosy.platform.vendorId
import com.nendo.argosy.platform.SOURCE_CLASS_JOYSTICK
import com.nendo.argosy.platform.SOURCE_JOYSTICK
import com.nendo.argosy.platform.PlatformInputEvent as InputEvent
import com.nendo.argosy.platform.device
import com.nendo.argosy.platform.deviceId
import com.nendo.argosy.platform.eventTime
import com.nendo.argosy.platform.isFromSource
import com.nendo.argosy.platform.source
import com.nendo.argosy.platform.PlatformKeyEvent as KeyEvent
import com.nendo.argosy.platform.action
import com.nendo.argosy.platform.downTime
import com.nendo.argosy.platform.keyCode
import com.nendo.argosy.platform.metaState
import com.nendo.argosy.platform.repeatCount
import com.nendo.argosy.platform.scanCode
import com.nendo.argosy.core.input.AndroidKeyCodes
import com.nendo.argosy.platform.PlatformMotionEvent as MotionEvent
import com.nendo.argosy.platform.actionMasked
import com.nendo.argosy.platform.getAxisValue
import com.nendo.argosy.core.input.AndroidMotionAxes
import com.nendo.argosy.core.input.ConnectedControllerTracker
import com.nendo.argosy.core.input.VirtualPad
import com.nendo.argosy.data.preferences.UserPreferencesRepository
import com.nendo.argosy.util.SafeCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface RawInputInterceptor {
    fun setRawKeyEventListener(listener: ((KeyEvent) -> Boolean)?)
    fun setRawMotionEventListener(listener: ((MotionEvent) -> Boolean)?)
    fun mapKeyToEvent(keyCode: Int): GamepadEvent?
}

@Singleton
class GamepadInputHandler @Inject constructor(
    preferencesRepository: UserPreferencesRepository,
    private val controllerTracker: ConnectedControllerTracker
) : RawInputInterceptor {

    private val _events = MutableSharedFlow<GamepadInput>(extraBufferCapacity = 16)
    private val _homeEvents = Channel<Unit>(Channel.BUFFERED)
    private val _shortcutEvents = Channel<UiShortcut>(Channel.BUFFERED)
    private val scope = SafeCoroutineScope(Dispatchers.Main.immediate, "GamepadInputHandler")

    var homeEventEnabled: Boolean = true

    private var swapAB = false
    private var swapXY = false
    private var swapStartSelect = false
    private var openNavigationKey: Int? = null
    private var openQuickPanelKey: Int? = null

    private var shortcutGate: UiShortcutGate? = null
    private var claimedShortcutKey: Int? = null

    private val selectModifier = SelectModifier()

    private var rawKeyEventListener: ((KeyEvent) -> Boolean)? = null
    private var rawMotionEventListener: ((MotionEvent) -> Boolean)? = null

    init {
        scope.launch {
            preferencesRepository.preferences.collect { prefs ->
                swapAB = prefs.swapAB
                swapXY = prefs.swapXY
                swapStartSelect = prefs.swapStartSelect
                selectModifier.comboMap = SelectModifier.comboMapFrom(prefs.selectLCombo, prefs.selectRCombo)
                openNavigationKey = prefs.openNavigationKey.takeIf(UiShortcutKeys::isBindable)
                openQuickPanelKey = prefs.openQuickPanelKey.takeIf(UiShortcutKeys::isBindable)
            }
        }
    }

    fun eventFlow(): Flow<GamepadInput> = _events.asSharedFlow()
    fun homeEventFlow(): Flow<Unit> = _homeEvents.receiveAsFlow()
    fun shortcutEventFlow(): Flow<UiShortcut> = _shortcutEvents.receiveAsFlow()

    fun attachShortcutGate(gate: UiShortcutGate) {
        shortcutGate = gate
    }

    fun detachShortcutGate(gate: UiShortcutGate) {
        if (shortcutGate === gate) shortcutGate = null
    }

    /**
     * Credits [event] to the virtual pad latch and the active controller. Call it for input this
     * handler never receives directly, such as a key forwarded to the companion screen.
     */
    fun observeInput(event: InputEvent) {
        VirtualPad.observe(event)
        controllerTracker.noteInput(event)
    }

    fun injectEvent(event: GamepadEvent, isRepeat: Boolean) {
        emitWithDebounce(event, isRepeat)
    }

    private val lastInputTimes = mutableMapOf<GamepadEvent, Long>()
    private val inputDebounceMs = 140L
    private var inputBlockedUntil = 0L

    fun blockInputFor(durationMs: Long) {
        inputBlockedUntil = System.currentTimeMillis() + durationMs
    }

    fun emitHomeEvent() {
        if (homeEventEnabled) {
            _homeEvents.trySend(Unit)
        }
    }

    override fun setRawKeyEventListener(listener: ((KeyEvent) -> Boolean)?) {
        rawKeyEventListener = listener
        if (listener != null) {
            selectHoldJob?.cancel()
            selectModifier.reset()
            releaseBackPress()
            claimedShortcutKey = null
        }
    }

    override fun setRawMotionEventListener(listener: ((MotionEvent) -> Boolean)?) {
        rawMotionEventListener = listener
        if (listener != null) resetStickMotion()
    }

    private var confirmDownTime = 0L
    private var confirmFired = false
    private var confirmDeferred = false
    private var confirmDeferJob: Job? = null
    private var selectHoldJob: Job? = null
    private var backPressClaimed = false
    private var backDeferred = false
    private var backDeferJob: Job? = null
    private val longPressThresholdMs = 500L

    private val padDirections = mutableMapOf<Int, AxisDirectionTracker>()
    private var heldDeviceId: Int? = null
    private var heldDirection: GamepadEvent? = null
    private var directionRepeatJob: Job? = null
    private val directionRepeatDelayMs = 400L
    private val directionRepeatIntervalMs = 150L

    /**
     * Left-stick sample to directional event conversion once a raw motion listener declines it.
     * [deliver] gets a direction entered with isRepeat false, then the held direction per repeat
     * tick with isRepeat true until neutral. The result is true while the stick holds a direction,
     * so a hat sample with the stick at rest reaches the platform's DPAD key synthesis.
     */
    fun processStickMotion(
        event: MotionEvent,
        steerRightStick: Boolean,
        deliver: (GamepadEvent, Boolean) -> Unit
    ): Boolean {
        observeInput(event)
        if (!event.isFromSource(SOURCE_CLASS_JOYSTICK)) return false
        if (handleMotionEvent(event)) {
            resetStickMotion()
            return true
        }
        if (steerRightStick) processRightStickMotion(event) else releaseRightStick()

        val deviceId = event.deviceId
        val stick = padDirections.getOrPut(deviceId) { AxisDirectionTracker() }
        val stickEdge = stick.update(
            event.getAxisValue(AndroidMotionAxes.AXIS_X),
            event.getAxisValue(AndroidMotionAxes.AXIS_Y)
        )
        val direction = stick.direction
        if (direction == null) padDirections.remove(deviceId)
        val ownerId = heldDeviceId
        if (direction == null && ownerId != null && ownerId != deviceId) return false

        heldDeviceId = if (direction == null) null else deviceId
        updateDirectionRepeat(direction, deliver)

        stickEdge?.let { deliver(it, false) }
        return direction != null
    }

    fun resetStickMotion() {
        padDirections.clear()
        heldDeviceId = null
        heldDirection = null
        directionRepeatJob?.cancel()
        directionRepeatJob = null
        releaseRightStick()
    }

    private fun updateDirectionRepeat(direction: GamepadEvent?, deliver: (GamepadEvent, Boolean) -> Unit) {
        if (direction == heldDirection) return
        heldDirection = direction
        directionRepeatJob?.cancel()
        directionRepeatJob = if (direction == null) {
            null
        } else {
            scope.launch {
                delay(directionRepeatDelayMs)
                while (isActive) {
                    deliver(direction, true)
                    delay(directionRepeatIntervalMs)
                }
            }
        }
    }

    private val rightStick = RightStickStepper(scope) { event, isRepeat -> emitWithDebounce(event, isRepeat) }
    private val rightStickHorizontal = RightStickStepper(scope, ::rightStickHorizontalStepFor) { event, isRepeat ->
        emitWithDebounce(event, isRepeat)
    }

    private fun processRightStickMotion(event: MotionEvent) {
        if (event.source and SOURCE_JOYSTICK == 0) return
        rightStick.onAxis(event.getAxisValue(AndroidMotionAxes.AXIS_RZ))
        rightStickHorizontal.onAxis(event.getAxisValue(AndroidMotionAxes.AXIS_Z))
    }

    private fun releaseRightStick() {
        rightStick.release()
        rightStickHorizontal.release()
    }

    fun handleMotionEvent(event: MotionEvent): Boolean {
        rawMotionEventListener?.let { listener ->
            return listener(event)
        }
        return false
    }

    /**
     * Confirm is deferred by the long-press threshold on a coroutine timer rather than decided on
     * release: controllers send repeated ACTION_DOWN and an unreliable ACTION_UP, so the timer
     * firing while the button is still held is what turns a Confirm into a LongConfirm. Back is
     * deferred the same way whenever the attached [UiShortcutGate] allows a long Back, and a hold
     * past the threshold opens the quick panel instead of going back.
     */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.action == AndroidKeyCodes.ACTION_DOWN) {
            observeInput(event)
            com.nendo.argosy.util.Logger.verbose("GamepadInput") { "KeyEvent: keyCode=${event.keyCode}, scanCode=${event.scanCode}, device=${event.device?.name}, deviceId=${event.deviceId}, source=${event.source}" }
        }

        rawKeyEventListener?.let { listener ->
            return listener(event)
        }

        if (claimShortcutKey(event)) return true

        val gamepadEvent = mapKeyToEvent(event.keyCode) ?: return false

        if (gamepadEvent == GamepadEvent.Back && handleBackPress(event)) return true

        if (gamepadEvent == GamepadEvent.Confirm && event.action == AndroidKeyCodes.ACTION_DOWN) {
            if (event.repeatCount == 0) {
                confirmDownTime = System.currentTimeMillis()
                confirmFired = false
                confirmDeferred = true
                confirmDeferJob?.cancel()
                confirmDeferJob = scope.launch {
                    delay(longPressThresholdMs)
                    if (confirmDeferred && !confirmFired) {
                        confirmFired = true
                        confirmDeferred = false
                        emitWithDebounce(GamepadEvent.LongConfirm)
                    }
                }
                return true
            } else if (confirmDeferred) {
                return true
            } else {
                return true
            }
        }
        if (gamepadEvent == GamepadEvent.Confirm && event.action == AndroidKeyCodes.ACTION_UP) {
            confirmDeferJob?.cancel()
            if (confirmDeferred && !confirmFired) {
                confirmFired = true
                confirmDeferred = false
                emitWithDebounce(GamepadEvent.Confirm)
            }
            return true
        }

        val isRepeat = event.repeatCount > 0
        val holdArmed = gamepadEvent == GamepadEvent.Select &&
            event.action == AndroidKeyCodes.ACTION_DOWN &&
            !isRepeat &&
            com.nendo.argosy.ui.dualscreen.selectHoldSwapsRoles()
        if (gamepadEvent == GamepadEvent.Select) trackSelectHold(event.action, isRepeat, holdArmed)

        val emitted = selectModifier.filter(gamepadEvent, event.action, isRepeat, holdArmed) ?: return true
        if (event.action == AndroidKeyCodes.ACTION_UP) {
            emitWithDebounce(emitted)
            return true
        }

        val signature = InputSignature.of(event)
        emitWithDebounce(emitted, isRepeat, signature)
        return true
    }

    private fun shortcutFor(keyCode: Int): UiShortcut? = when (keyCode) {
        openNavigationKey -> UiShortcut.OPEN_NAVIGATION
        openQuickPanelKey -> UiShortcut.OPEN_QUICK_PANEL
        else -> null
    }

    private fun claimShortcutKey(event: KeyEvent): Boolean {
        val shortcut = shortcutFor(event.keyCode) ?: return false
        return when (event.action) {
            AndroidKeyCodes.ACTION_DOWN -> when {
                event.repeatCount > 0 -> claimedShortcutKey == event.keyCode
                shortcutGate?.hotkeysAllowed() != true -> false
                else -> {
                    claimedShortcutKey = event.keyCode
                    _shortcutEvents.trySend(shortcut)
                    true
                }
            }
            AndroidKeyCodes.ACTION_UP -> {
                if (claimedShortcutKey != event.keyCode) {
                    false
                } else {
                    claimedShortcutKey = null
                    true
                }
            }
            else -> false
        }
    }

    private fun handleBackPress(event: KeyEvent): Boolean = when (event.action) {
        AndroidKeyCodes.ACTION_DOWN -> when {
            event.repeatCount > 0 -> backPressClaimed
            shortcutGate?.longBackAllowed() != true -> {
                releaseBackPress()
                false
            }
            else -> {
                deferBackPress()
                true
            }
        }
        AndroidKeyCodes.ACTION_UP -> {
            if (!backPressClaimed) {
                false
            } else {
                val shortPress = backDeferred
                releaseBackPress()
                if (shortPress) emitWithDebounce(GamepadEvent.Back)
                true
            }
        }
        else -> backPressClaimed
    }

    private fun deferBackPress() {
        backDeferJob?.cancel()
        backPressClaimed = true
        backDeferred = true
        backDeferJob = scope.launch {
            kotlinx.coroutines.delay(longPressThresholdMs)
            if (!backDeferred) return@launch
            backDeferred = false
            if (shortcutGate?.longBackAllowed() == true) {
                _shortcutEvents.trySend(UiShortcut.OPEN_QUICK_PANEL)
            } else {
                emitWithDebounce(GamepadEvent.Back)
            }
        }
    }

    private fun releaseBackPress() {
        backDeferJob?.cancel()
        backDeferJob = null
        backPressClaimed = false
        backDeferred = false
    }

    private fun trackSelectHold(action: Int, isRepeat: Boolean, holdArmed: Boolean) {
        if (action == AndroidKeyCodes.ACTION_UP) {
            selectHoldJob?.cancel()
            return
        }
        if (action != AndroidKeyCodes.ACTION_DOWN || isRepeat) return
        selectHoldJob?.cancel()
        if (!holdArmed) return
        selectHoldJob = scope.launch {
            kotlinx.coroutines.delay(longPressThresholdMs)
            if (selectModifier.claimHold()) emitWithDebounce(GamepadEvent.LongSelect)
        }
    }

    private fun emitWithDebounce(
        event: GamepadEvent,
        isRepeat: Boolean = false,
        signature: InputSignature? = null
    ) {
        val currentTime = System.currentTimeMillis()
        if (currentTime < inputBlockedUntil) return
        val lastTime = lastInputTimes[event] ?: 0L
        if (currentTime - lastTime < inputDebounceMs) return
        lastInputTimes[event] = currentTime
        _events.tryEmit(GamepadInput(event, isRepeat, signature))
    }

    override fun mapKeyToEvent(keyCode: Int): GamepadEvent? =
        mapKeycodeToGamepadEvent(keyCode, swapAB, swapXY, swapStartSelect)
}
