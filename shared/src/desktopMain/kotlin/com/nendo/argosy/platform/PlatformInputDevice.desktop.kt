package com.nendo.argosy.platform

import java.util.concurrent.ConcurrentHashMap

actual class PlatformInputDevice(
    internal val deviceId: Int,
    internal val deviceName: String,
    internal val deviceVendorId: Int,
    internal val deviceProductId: Int,
    internal val deviceDescriptor: String,
    internal val deviceSources: Int = SOURCE_GAMEPAD or SOURCE_JOYSTICK,
    internal val deviceControllerNumber: Int = 0,
)

actual val PlatformInputDevice.id: Int get() = deviceId

actual val PlatformInputDevice.name: String get() = deviceName

actual val PlatformInputDevice.vendorId: Int get() = deviceVendorId

actual val PlatformInputDevice.productId: Int get() = deviceProductId

actual val PlatformInputDevice.descriptor: String get() = deviceDescriptor

actual val PlatformInputDevice.sources: Int get() = deviceSources

actual val PlatformInputDevice.controllerNumber: Int get() = deviceControllerNumber

actual val PlatformInputDevice.isVirtual: Boolean get() = false

actual val PlatformInputDevice.isExternal: Boolean get() = true

actual val PlatformInputDevice.isEnabled: Boolean get() = true

actual object PlatformInputDevices {
    private val devices = ConcurrentHashMap<Int, PlatformInputDevice>()

    fun connected(device: PlatformInputDevice) {
        devices[device.deviceId] = device
    }

    fun disconnected(id: Int) {
        devices.remove(id)
    }

    actual fun getDevice(id: Int): PlatformInputDevice? = devices[id]
    actual fun getDeviceIds(): IntArray = devices.keys.sorted().toIntArray()
}
