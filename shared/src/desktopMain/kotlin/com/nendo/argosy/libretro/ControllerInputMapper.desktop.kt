package com.nendo.argosy.libretro

import com.nendo.argosy.platform.PlatformInputDevice as InputDevice
import com.nendo.argosy.data.repository.InputSource
import javax.inject.Inject

actual class ControllerInputMapper @Inject constructor() {
    actual companion object {
        actual fun resolveMappedKey(mapping: Map<InputSource, Int>?, keyCode: Int): Int = 0

        actual fun retroButtonToAndroidKeyCode(retroButton: Int): Int = 0
    }

    actual fun setPortResolver(resolver: (InputDevice) -> Int) {
    }

    actual fun setExtendedMappings(mappings: Map<String, Map<InputSource, Int>>) {
    }

    actual fun setExtendedMappingForController(controllerId: String, mapping: Map<InputSource, Int>) {
    }

    actual fun clearMappings() {
    }

    actual fun hasAnalogMappingForAxis(device: InputDevice, axis: Int): Boolean = false

    actual fun retroButtonFor(device: InputDevice, keyCode: Int): Int? = null

    actual fun getMappedButtonsForController(controllerId: String): Set<Int> = emptySet()
}
