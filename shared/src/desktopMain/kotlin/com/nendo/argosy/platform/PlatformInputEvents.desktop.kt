package com.nendo.argosy.platform

actual abstract class PlatformInputEvent(
    internal val eventDeviceId: Int,
    internal val eventSource: Int,
    internal val eventTimeMs: Long,
)

actual open class PlatformKeyEvent(
    deviceId: Int,
    source: Int,
    eventTime: Long,
    internal val eventKeyCode: Int,
    internal val eventAction: Int,
    internal val eventRepeatCount: Int = 0,
    internal val eventDownTime: Long = eventTime,
    internal val eventScanCode: Int = 0,
    internal val eventMetaState: Int = 0,
) : PlatformInputEvent(deviceId, source, eventTime)

actual class PlatformMotionEvent(
    deviceId: Int,
    source: Int,
    eventTime: Long,
    internal val eventAction: Int,
    internal val axisValues: Map<Int, Float>,
) : PlatformInputEvent(deviceId, source, eventTime)

actual val PlatformInputEvent.deviceId: Int get() = eventDeviceId

actual val PlatformInputEvent.device: PlatformInputDevice? get() = PlatformInputDevices.getDevice(eventDeviceId)

actual val PlatformInputEvent.source: Int get() = eventSource

actual val PlatformInputEvent.eventTime: Long get() = eventTimeMs

actual fun PlatformInputEvent.isFromSource(source: Int): Boolean = eventSource and source == source

actual val PlatformKeyEvent.keyCode: Int get() = eventKeyCode

actual val PlatformKeyEvent.action: Int get() = eventAction

actual val PlatformKeyEvent.repeatCount: Int get() = eventRepeatCount

actual val PlatformKeyEvent.scanCode: Int get() = eventScanCode

actual val PlatformKeyEvent.metaState: Int get() = eventMetaState

actual val PlatformKeyEvent.downTime: Long get() = eventDownTime

actual val PlatformMotionEvent.action: Int get() = eventAction

actual val PlatformMotionEvent.actionMasked: Int get() = eventAction and 0xff

actual fun PlatformMotionEvent.getAxisValue(axis: Int): Float = axisValues[axis] ?: 0f
