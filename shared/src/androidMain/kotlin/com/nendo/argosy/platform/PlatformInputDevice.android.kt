package com.nendo.argosy.platform

import android.view.InputDevice

actual typealias PlatformInputDevice = InputDevice

actual val PlatformInputDevice.id: Int get() = id

actual val PlatformInputDevice.name: String get() = name

actual val PlatformInputDevice.vendorId: Int get() = vendorId

actual val PlatformInputDevice.productId: Int get() = productId

actual val PlatformInputDevice.descriptor: String get() = descriptor

actual val PlatformInputDevice.sources: Int get() = sources

actual val PlatformInputDevice.controllerNumber: Int get() = controllerNumber

actual val PlatformInputDevice.isVirtual: Boolean get() = isVirtual

actual val PlatformInputDevice.isExternal: Boolean get() = isExternal

actual val PlatformInputDevice.isEnabled: Boolean get() = isEnabled

actual object PlatformInputDevices {
    actual fun getDevice(id: Int): PlatformInputDevice? = InputDevice.getDevice(id)
    actual fun getDeviceIds(): IntArray = InputDevice.getDeviceIds() ?: IntArray(0)
}
