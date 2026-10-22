package com.nendo.argosy.ui.input

import com.nendo.argosy.core.input.AndroidKeyCodes
import com.nendo.argosy.core.input.AndroidMotionAxes
import com.nendo.argosy.platform.SOURCE_GAMEPAD
import com.nendo.argosy.platform.device
import com.nendo.argosy.platform.eventTime
import com.nendo.argosy.platform.getAxisValue
import com.nendo.argosy.platform.id
import javax.inject.Inject
import javax.inject.Singleton
import com.nendo.argosy.platform.PlatformKeyEvent as KeyEvent
import com.nendo.argosy.platform.PlatformMotionEvent as MotionEvent

@Singleton
actual class TriggerAxisKeyEmitter @Inject constructor() {
    private val pressedState = mutableMapOf<Pair<Int, Int>, Boolean>()

    actual fun emit(event: MotionEvent): List<KeyEvent> = emitByAxis(event).map { it.second }

    actual fun emitByAxis(event: MotionEvent): List<Pair<Int, KeyEvent>> {
        val device = event.device ?: return emptyList()
        val deviceId = device.id
        val results = mutableListOf<Pair<Int, KeyEvent>>()
        for ((axis, keyCode) in TRIGGER_AXES) {
            val isPressed = event.getAxisValue(axis) > THRESHOLD
            val key = deviceId to axis
            val wasPressed = pressedState[key] ?: false
            if (isPressed != wasPressed) {
                pressedState[key] = isPressed
                val action = if (isPressed) AndroidKeyCodes.ACTION_DOWN else AndroidKeyCodes.ACTION_UP
                results.add(axis to buildKeyEvent(event, deviceId, action, keyCode))
            }
        }
        return results
    }

    actual fun forget(deviceId: Int) {
        pressedState.keys.removeAll { it.first == deviceId }
    }

    private fun buildKeyEvent(source: MotionEvent, deviceId: Int, action: Int, keyCode: Int): KeyEvent =
        KeyEvent(
            deviceId = deviceId,
            source = SOURCE_GAMEPAD,
            eventTime = source.eventTime,
            eventKeyCode = keyCode,
            eventAction = action,
        )

    actual companion object {
        private const val THRESHOLD = 0.5f
        private val TRIGGER_AXES = listOf(
            AndroidMotionAxes.AXIS_LTRIGGER to AndroidKeyCodes.KEYCODE_BUTTON_L2,
            AndroidMotionAxes.AXIS_BRAKE to AndroidKeyCodes.KEYCODE_BUTTON_L2,
            AndroidMotionAxes.AXIS_RTRIGGER to AndroidKeyCodes.KEYCODE_BUTTON_R2,
            AndroidMotionAxes.AXIS_GAS to AndroidKeyCodes.KEYCODE_BUTTON_R2,
        )

        actual fun keyCodeForAxis(axis: Int): Int? = TRIGGER_AXES.firstOrNull { it.first == axis }?.second
    }
}
