package com.nendo.argosy.core.input

import com.nendo.argosy.platform.PlatformInputDevice as InputDevice
import com.nendo.argosy.platform.SOURCE_GAMEPAD
import com.nendo.argosy.platform.SOURCE_JOYSTICK
import com.nendo.argosy.platform.isVirtual
import com.nendo.argosy.platform.sources

actual fun InputDevice.actsAsGamepad(): Boolean = isPhysicalGamepad()

actual fun InputDevice.isPhysicalGamepad(): Boolean =
    !isVirtual && (sources and SOURCE_GAMEPAD == SOURCE_GAMEPAD || sources and SOURCE_JOYSTICK == SOURCE_JOYSTICK)
