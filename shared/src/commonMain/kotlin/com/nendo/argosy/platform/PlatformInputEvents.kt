package com.nendo.argosy.platform

expect abstract class PlatformInputEvent

expect open class PlatformKeyEvent : PlatformInputEvent

expect class PlatformMotionEvent : PlatformInputEvent

expect val PlatformInputEvent.deviceId: Int

expect val PlatformInputEvent.device: PlatformInputDevice?

expect val PlatformInputEvent.source: Int

expect val PlatformInputEvent.eventTime: Long

expect fun PlatformInputEvent.isFromSource(source: Int): Boolean

expect val PlatformKeyEvent.keyCode: Int

expect val PlatformKeyEvent.action: Int

expect val PlatformKeyEvent.repeatCount: Int

expect val PlatformKeyEvent.scanCode: Int

expect val PlatformKeyEvent.metaState: Int

expect val PlatformKeyEvent.downTime: Long

expect val PlatformMotionEvent.action: Int

expect val PlatformMotionEvent.actionMasked: Int

expect fun PlatformMotionEvent.getAxisValue(axis: Int): Float
