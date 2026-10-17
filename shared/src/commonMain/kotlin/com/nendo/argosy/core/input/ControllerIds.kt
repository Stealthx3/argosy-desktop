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

fun controllerIdOf(device: InputDevice): String =
    "${device.vendorId}:${device.productId}:${device.descriptor}"
