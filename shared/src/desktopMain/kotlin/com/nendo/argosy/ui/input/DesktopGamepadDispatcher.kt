package com.nendo.argosy.ui.input

import com.nendo.argosy.core.input.AndroidKeyCodes
import com.nendo.argosy.core.input.AndroidMotionAxes
import com.nendo.argosy.core.input.ConnectedControllerTracker
import com.nendo.argosy.platform.PlatformInputDevice
import com.nendo.argosy.platform.PlatformInputDevices
import com.nendo.argosy.platform.PlatformKeyEvent
import com.nendo.argosy.platform.PlatformMotionEvent
import com.nendo.argosy.platform.SOURCE_DPAD
import com.nendo.argosy.platform.SOURCE_GAMEPAD
import com.nendo.argosy.platform.SOURCE_JOYSTICK
import com.nendo.argosy.platform.sdl.Sdl3
import com.nendo.argosy.platform.sdl.SdlGamepadSource
import com.nendo.argosy.platform.sdl.SdlPadEvent
import com.nendo.argosy.util.Logger
import java.awt.EventQueue
import javax.inject.Inject
import javax.inject.Singleton
import javax.swing.Timer

/**
 * Desktop counterpart of ArgosyActivity's gamepad path: SDL3 pads become input devices, their
 * buttons Android key events (with Android's key repeat) through [DesktopKeyDispatcher], and their
 * sticks and triggers motion events through the trigger emitter and the handler's stick processing,
 * as dispatchGenericMotionEvent does. Everything after the SDL thread runs on the AWT event thread.
 */
@Singleton
class DesktopGamepadDispatcher @Inject constructor(
    private val keyDispatcher: DesktopKeyDispatcher,
    private val gamepadInputHandler: GamepadInputHandler,
    private val triggerAxisKeyEmitter: TriggerAxisKeyEmitter,
    private val controllerTracker: ConnectedControllerTracker,
) {
    private val axisValues = HashMap<Int, HashMap<Int, Float>>()
    private val downTimes = HashMap<Pair<Int, Int>, Long>()
    private var repeating: RepeatingKey? = null
    private var source: SdlGamepadSource? = null

    fun start(): Boolean {
        if (source != null) return true
        val sdl = Sdl3.load()
        if (sdl == null) {
            Logger.warn(TAG, "SDL3 not found; gamepads unavailable")
            return false
        }
        source = SdlGamepadSource(sdl) { event -> EventQueue.invokeLater { handle(event) } }.also { it.start() }
        return true
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
        Logger.info(TAG, "gamepad added: ${event.name} (${event.vendorId}:${event.productId}) id=$deviceId")
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
        if (repeating?.deviceId == deviceId) stopRepeat()
        axisValues.remove(deviceId)
        downTimes.keys.removeAll { it.first == deviceId }
        triggerAxisKeyEmitter.forget(deviceId)
        PlatformInputDevices.disconnected(deviceId)
        controllerTracker.refresh()
    }

    private fun button(deviceId: Int, event: SdlPadEvent.Button) {
        val keyCode = SdlGamepadMapping.keyCode(event.button) ?: return
        val source = if (SdlGamepadMapping.isDpad(event.button)) SOURCE_GAMEPAD or SOURCE_DPAD else SOURCE_GAMEPAD
        val now = System.currentTimeMillis()
        val key = deviceId to keyCode
        if (event.down) {
            downTimes[key] = now
            stopRepeat()
            val down = PlatformKeyEvent(deviceId, source, now, keyCode, AndroidKeyCodes.ACTION_DOWN, 0, now)
            keyDispatcher.deliver(down)
            startRepeat(RepeatingKey(deviceId, source, keyCode, now))
        } else {
            val downTime = downTimes.remove(key) ?: now
            if (repeating?.let { it.deviceId == deviceId && it.keyCode == keyCode } == true) stopRepeat()
            keyDispatcher.deliver(
                PlatformKeyEvent(deviceId, source, now, keyCode, AndroidKeyCodes.ACTION_UP, 0, downTime),
            )
        }
    }

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
        triggerAxisKeyEmitter.emit(motion).forEach { keyDispatcher.deliver(it) }
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

    private class RepeatingKey(val deviceId: Int, val source: Int, val keyCode: Int, val downTime: Long) {
        var repeatCount = 0
        var timer: Timer? = null
    }

    private companion object {
        const val TAG = "DesktopGamepad"
        const val REPEAT_TIMEOUT_MS = 400
        const val REPEAT_INTERVAL_MS = 50
    }
}
