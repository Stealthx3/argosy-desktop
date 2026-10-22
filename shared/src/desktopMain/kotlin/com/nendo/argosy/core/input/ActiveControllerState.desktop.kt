package com.nendo.argosy.core.input

import com.nendo.argosy.platform.PlatformInputDevice as InputDevice
import com.nendo.argosy.platform.PlatformInputEvent as InputEvent
import com.nendo.argosy.platform.PlatformKeyEvent as KeyEvent
import com.nendo.argosy.platform.PlatformMotionEvent as MotionEvent
import com.nendo.argosy.platform.action
import com.nendo.argosy.platform.device
import com.nendo.argosy.platform.deviceId
import com.nendo.argosy.platform.getAxisValue
import com.nendo.argosy.platform.id
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

fun InputDevice.toActiveController(): ActiveController = ActiveController(id, controllerIdOf(this))

private const val ACTIVATING_DEFLECTION = 0.5f

private val ACTIVATING_AXES = intArrayOf(
    AndroidMotionAxes.AXIS_X,
    AndroidMotionAxes.AXIS_Y,
    AndroidMotionAxes.AXIS_Z,
    AndroidMotionAxes.AXIS_RZ,
    AndroidMotionAxes.AXIS_HAT_X,
    AndroidMotionAxes.AXIS_HAT_Y,
    AndroidMotionAxes.AXIS_LTRIGGER,
    AndroidMotionAxes.AXIS_RTRIGGER,
    AndroidMotionAxes.AXIS_BRAKE,
    AndroidMotionAxes.AXIS_GAS
)

/**
 * Desktop copy of the Android ActiveControllerState (androidMain core/input/ActiveController.kt):
 * the pad that last sent input. Events from a device that is not a registered pad (the keyboard)
 * never take the slot.
 */
class ActiveControllerState {
    private val _active = MutableStateFlow<ActiveController?>(null)
    val active: StateFlow<ActiveController?> = _active.asStateFlow()

    @Volatile
    private var held = false

    fun setHeld(held: Boolean) {
        this.held = held
    }

    fun noteInput(event: InputEvent) {
        if (held) return
        if (event is KeyEvent && event.action != AndroidKeyCodes.ACTION_DOWN) return
        val deviceId = event.deviceId
        if (_active.value?.deviceId == deviceId) return
        if (event is MotionEvent && !event.isDeflected()) return
        val device = event.device ?: return
        if (!device.actsAsGamepad()) return
        _active.value = device.toActiveController()
    }

    fun onPadsChanged(connected: List<ActiveController>) {
        val current = _active.value ?: return
        if (connected.any { it.deviceId == current.deviceId }) return
        _active.value = connected.firstOrNull()
    }

    private fun MotionEvent.isDeflected(): Boolean =
        ACTIVATING_AXES.any { abs(getAxisValue(it)) >= ACTIVATING_DEFLECTION }
}
