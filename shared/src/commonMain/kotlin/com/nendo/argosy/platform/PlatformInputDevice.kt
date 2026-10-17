package com.nendo.argosy.platform

const val SOURCE_CLASS_BUTTON: Int = 1

const val SOURCE_CLASS_JOYSTICK: Int = 16

const val SOURCE_DPAD: Int = 513

const val SOURCE_GAMEPAD: Int = 1025

const val SOURCE_JOYSTICK: Int = 16777232

const val SOURCE_KEYBOARD: Int = 257

expect class PlatformInputDevice

expect val PlatformInputDevice.id: Int

expect val PlatformInputDevice.name: String

expect val PlatformInputDevice.vendorId: Int

expect val PlatformInputDevice.productId: Int

expect val PlatformInputDevice.descriptor: String

expect val PlatformInputDevice.sources: Int

expect val PlatformInputDevice.controllerNumber: Int

expect val PlatformInputDevice.isVirtual: Boolean

expect val PlatformInputDevice.isExternal: Boolean

expect val PlatformInputDevice.isEnabled: Boolean

expect object PlatformInputDevices {
    fun getDevice(id: Int): PlatformInputDevice?
    fun getDeviceIds(): IntArray
}
