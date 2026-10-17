package com.nendo.argosy.libretro

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
import com.nendo.argosy.platform.PlatformMotionEvent as MotionEvent
import com.nendo.argosy.platform.action
import com.nendo.argosy.platform.actionMasked
import com.nendo.argosy.platform.device
import com.nendo.argosy.platform.deviceId
import com.nendo.argosy.platform.eventTime
import com.nendo.argosy.platform.getAxisValue
import com.nendo.argosy.platform.isFromSource
import com.nendo.argosy.platform.source
import com.nendo.argosy.core.input.AndroidMotionAxes
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

    fun processMotionEvent(event: MotionEvent): List<SyntheticKeyEvent>

    fun hasAnalogMappingForAxis(device: InputDevice, axis: Int): Boolean

    fun retroButtonFor(device: InputDevice, keyCode: Int): Int?

    fun getMappedButtonsForController(controllerId: String): Set<Int>
}
