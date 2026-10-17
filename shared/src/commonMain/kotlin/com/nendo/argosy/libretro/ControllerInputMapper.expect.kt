package com.nendo.argosy.libretro

import com.nendo.argosy.platform.PlatformInputDevice as InputDevice
import com.nendo.argosy.data.repository.InputSource

expect class ControllerInputMapper {
    companion object {
        fun resolveMappedKey(mapping: Map<InputSource, Int>?, keyCode: Int): Int

        fun retroButtonToAndroidKeyCode(retroButton: Int): Int
    }

    fun setPortResolver(resolver: (InputDevice) -> Int)

    fun setExtendedMappings(mappings: Map<String, Map<InputSource, Int>>)

    fun setExtendedMappingForController(controllerId: String, mapping: Map<InputSource, Int>)

    fun clearMappings()

    fun hasAnalogMappingForAxis(device: InputDevice, axis: Int): Boolean

    fun retroButtonFor(device: InputDevice, keyCode: Int): Int?

    fun getMappedButtonsForController(controllerId: String): Set<Int>
}
