package com.nendo.argosy.ui.input

import com.nendo.argosy.core.input.AndroidKeyCodes
import com.nendo.argosy.core.input.AndroidMotionAxes
import com.nendo.argosy.core.input.ConnectedControllerTracker
import com.nendo.argosy.data.preferences.ControllerInputMode
import com.nendo.argosy.data.preferences.ControlsPreferencesRepository
import com.nendo.argosy.platform.ControllerInputSupport
import com.nendo.argosy.platform.PlatformInputDevice
import com.nendo.argosy.platform.PlatformInputDevices
import com.nendo.argosy.platform.PlatformKeyEvent
import com.nendo.argosy.platform.PlatformMotionEvent
import com.nendo.argosy.platform.SOURCE_DPAD
import com.nendo.argosy.platform.SOURCE_GAMEPAD
import com.nendo.argosy.platform.SOURCE_JOYSTICK
import com.nendo.argosy.platform.action
import com.nendo.argosy.platform.keyCode
import com.nendo.argosy.platform.sdl.Sdl3
import com.nendo.argosy.platform.sdl.SdlGamepadSource
import com.nendo.argosy.platform.sdl.SdlPadEvent
import com.nendo.argosy.platform.sdl.Sdl3Library
import com.nendo.argosy.util.Logger
import com.nendo.argosy.util.SafeCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.awt.EventQueue
import javax.inject.Inject
import javax.inject.Singleton
import javax.swing.Timer

/**
 * Desktop counterpart of ArgosyActivity's gamepad path: SDL3 pads become input devices, their
 * buttons Android key events (with Android's key repeat) through [DesktopKeyDispatcher], and their
 * sticks and triggers motion events through the trigger emitter and the handler's stick processing,
 * as dispatchGenericMotionEvent does. Everything after the SDL thread runs on the AWT event thread.
 * The SDL source follows the controller input mode setting and is restarted when it changes.
 */
@Singleton
class DesktopGamepadDispatcher @Inject constructor(
    private val keyDispatcher: DesktopKeyDispatcher,
    private val gamepadInputHandler: GamepadInputHandler,
    private val triggerAxisKeyEmitter: TriggerAxisKeyEmitter,
    private val controllerTracker: ConnectedControllerTracker,
    private val rumble: GamepadRumble,
    private val controlsPreferences: ControlsPreferencesRepository,
    private val remapper: NavigationButtonRemapper,
) {
    private val axisValues = HashMap<Int, HashMap<Int, Float>>()
    private val pressed = HashMap<Pair<Int, Int>, PressedKey>()
    private val controllerKeys = HashMap<Int, String>()
    private var repeating: RepeatingKey? = null
    private val connectedIds = LinkedHashSet<Int>()
    private val scope = SafeCoroutineScope(Dispatchers.Default, TAG)

    @Volatile
    private var source: SdlGamepadSource? = null

    @Volatile
    var sourceGeneration = 0
        private set

    fun start(): Boolean {
        if (source != null) return true
        val sdl = Sdl3.load()
        if (sdl == null) {
            Logger.warn(TAG, "SDL3 not found; gamepads unavailable")
            return false
        }
        val modes = controlsPreferences.preferences.map { it.controllerInputMode }.distinctUntilChanged()
        launchSource(sdl, runBlocking { modes.first() })
        scope.launch { modes.drop(1).collect { restart(sdl, it) } }
        return true
    }

    private fun launchSource(sdl: Sdl3Library, mode: ControllerInputMode) {
        val steamInputAvailable = ControllerInputSupport.steamInputAvailable
        val resolved = mode.resolved(steamInputAvailable)
        Logger.info(TAG, "input mode: setting=$mode steamInputAvailable=$steamInputAvailable using=$resolved")
        source = SdlGamepadSource(sdl, resolved) { event -> EventQueue.invokeLater { handle(event) } }.also {
            rumble.attach(it)
            it.start()
        }
        sourceGeneration++
    }

    private fun restart(sdl: Sdl3Library, mode: ControllerInputMode) {
        source?.stopAndJoin(STOP_TIMEOUT_MS)
        EventQueue.invokeAndWait { connectedIds.toList().forEach(::disconnected) }
        launchSource(sdl, mode)
    }

    fun awaitReady(timeoutMs: Long): Boolean = source?.awaitReady(timeoutMs) == true

    fun releaseAll() {
        stopRepeat()
    }

    internal fun handle(event: SdlPadEvent) {
        val deviceId = SdlGamepadMapping.deviceId(event.instanceId)
        when (event) {
            is SdlPadEvent.Added -> connected(deviceId, event)
            is SdlPadEvent.Removed -> disconnected(deviceId)
            is SdlPadEvent.Button -> button(deviceId, event)
            is SdlPadEvent.Axis -> axis(deviceId, event)
        }
    }

    private fun connected(deviceId: Int, event: SdlPadEvent.Added) {
        Logger.info(
            TAG,
            "gamepad added: ${event.name} (${"%04x".format(event.vendorId)}:${"%04x".format(event.productId)}) " +
                "type=${event.gamepadType} steamVirtual=${event.steamVirtual} mode=${source?.mode} id=$deviceId",
        )
        connectedIds += deviceId
        controllerKeys[deviceId] = NavigationButtonRemapper.controllerKey(event.descriptor)
        PlatformInputDevices.connected(
            PlatformInputDevice(
                deviceId = deviceId,
                deviceName = event.name,
                deviceVendorId = event.vendorId,
                deviceProductId = event.productId,
                deviceDescriptor = event.descriptor,
                deviceControllerNumber = SdlGamepadMapping.controllerNumber(event.playerIndex),
            ),
        )
        controllerTracker.refresh()
    }

    private fun disconnected(deviceId: Int) {
        Logger.info(TAG, "gamepad removed: id=$deviceId")
        connectedIds -= deviceId
        if (repeating?.deviceId == deviceId) stopRepeat()
        axisValues.remove(deviceId)
        pressed.keys.removeAll { it.first == deviceId }
        controllerKeys.remove(deviceId)
        triggerAxisKeyEmitter.forget(deviceId)
        PlatformInputDevices.disconnected(deviceId)
        controllerTracker.refresh()
    }

    private fun button(deviceId: Int, event: SdlPadEvent.Button) {
        val physical = SdlGamepadMapping.keyCode(event.button) ?: return
        press(deviceId, physical, event.down)
    }

    private fun press(deviceId: Int, physical: Int, down: Boolean) {
        val controller = controllerKeys[deviceId].orEmpty()
        val now = System.currentTimeMillis()
        val key = deviceId to physical
        if (down) {
            if (remapper.interceptDown(controller, physical)) return
            val keyCode = remapper.translate(controller, physical)
            val source = sourceFor(keyCode)
            pressed[key] = PressedKey(now, keyCode)
            stopRepeat()
            val down = PlatformKeyEvent(deviceId, source, now, keyCode, AndroidKeyCodes.ACTION_DOWN, 0, now)
            keyDispatcher.deliver(down)
            startRepeat(RepeatingKey(deviceId, source, keyCode, now))
        } else {
            if (remapper.interceptUp(controller, physical)) return
            val press = pressed.remove(key)
            val keyCode = press?.keyCode ?: remapper.translate(controller, physical)
            if (repeating?.let { it.deviceId == deviceId && it.keyCode == keyCode } == true) stopRepeat()
            keyDispatcher.deliver(
                PlatformKeyEvent(
                    deviceId, sourceFor(keyCode), now, keyCode, AndroidKeyCodes.ACTION_UP, 0, press?.downTime ?: now,
                ),
            )
        }
    }

    private fun sourceFor(keyCode: Int): Int =
        if (keyCode in DPAD_KEYCODES) SOURCE_GAMEPAD or SOURCE_DPAD else SOURCE_GAMEPAD

    private fun axis(deviceId: Int, event: SdlPadEvent.Axis) {
        val axis = SdlGamepadMapping.axis(event.axis) ?: return
        val values = axisValues.getOrPut(deviceId) { HashMap() }
        values[axis] = SdlGamepadMapping.axisValue(event.axis, event.value)
        val motion = PlatformMotionEvent(
            deviceId,
            SOURCE_JOYSTICK,
            System.currentTimeMillis(),
            AndroidMotionAxes.ACTION_MOVE,
            HashMap(values),
        )
        triggerAxisKeyEmitter.emit(motion).forEach {
            press(deviceId, it.keyCode, it.action == AndroidKeyCodes.ACTION_DOWN)
        }
        if (gamepadInputHandler.processStickMotion(motion, true, gamepadInputHandler::injectEvent)) return
        gamepadInputHandler.handleMotionEvent(motion)
    }

    private fun startRepeat(key: RepeatingKey) {
        repeating = key
        key.timer = Timer(REPEAT_INTERVAL_MS) {
            key.repeatCount++
            keyDispatcher.deliver(
                PlatformKeyEvent(
                    key.deviceId,
                    key.source,
                    System.currentTimeMillis(),
                    key.keyCode,
                    AndroidKeyCodes.ACTION_DOWN,
                    key.repeatCount,
                    key.downTime,
                ),
            )
        }.apply {
            initialDelay = REPEAT_TIMEOUT_MS
            start()
        }
    }

    private fun stopRepeat() {
        repeating?.timer?.stop()
        repeating = null
    }

    private class PressedKey(val downTime: Long, val keyCode: Int)

    private class RepeatingKey(val deviceId: Int, val source: Int, val keyCode: Int, val downTime: Long) {
        var repeatCount = 0
        var timer: Timer? = null
    }

    private companion object {
        const val TAG = "DesktopGamepad"
        const val REPEAT_TIMEOUT_MS = 400
        const val REPEAT_INTERVAL_MS = 50
        const val STOP_TIMEOUT_MS = 2_000L
        val DPAD_KEYCODES = setOf(
            AndroidKeyCodes.KEYCODE_DPAD_UP,
            AndroidKeyCodes.KEYCODE_DPAD_DOWN,
            AndroidKeyCodes.KEYCODE_DPAD_LEFT,
            AndroidKeyCodes.KEYCODE_DPAD_RIGHT,
        )
    }
}
