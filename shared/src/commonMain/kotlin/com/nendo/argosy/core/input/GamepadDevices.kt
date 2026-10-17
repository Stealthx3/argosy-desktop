package com.nendo.argosy.core.input

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
import com.nendo.argosy.platform.SOURCE_GAMEPAD
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

private const val OEM_VIRTUAL_DEVICE_PREFIX = "uinput-"

/**
 * Firmware that re-injects its built-in controls through Android's virtual device. The device
 * describes itself as a keyboard, so it counts as a pad only once it sends pad input: an event
 * that carries a pad source, or a gamepad button code.
 */
object VirtualPad {
    @Volatile
    var seen: Boolean = false
        private set

    fun observe(event: InputEvent): Boolean {
        if (seen || event.device?.isVirtual != true) return false
        if (!event.hasPadSource() && !(event is KeyEvent && event.keyCode.isGamepadButtonCode())) {
            return false
        }
        seen = true
        return true
    }

    private fun InputEvent.hasPadSource(): Boolean =
        source and SOURCE_GAMEPAD == SOURCE_GAMEPAD ||
            source and SOURCE_JOYSTICK == SOURCE_JOYSTICK

    private fun Int.isGamepadButtonCode(): Boolean =
        this in AndroidKeyCodes.KEYCODE_BUTTON_A..AndroidKeyCodes.KEYCODE_BUTTON_MODE
}

fun InputDevice.actsAsGamepad(): Boolean = isPhysicalGamepad() || (isVirtual && VirtualPad.seen)

/**
 * Whether a device is a pad the launcher should act on. The source flags alone are not enough:
 * HyperOS registers a `uinput-xiaomi` device that advertises a gamepad source with no controller
 * behind it, which hid the on-screen controls and claimed player one on a bare phone.
 */
fun InputDevice.isPhysicalGamepad(): Boolean {
    val padSource = sources and SOURCE_GAMEPAD == SOURCE_GAMEPAD ||
        sources and SOURCE_JOYSTICK == SOURCE_JOYSTICK
    if (!padSource || isVirtual) return false
    return !name.orEmpty().startsWith(OEM_VIRTUAL_DEVICE_PREFIX, ignoreCase = true)
}
