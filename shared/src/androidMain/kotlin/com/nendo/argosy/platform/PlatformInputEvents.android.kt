package com.nendo.argosy.platform

import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent

actual typealias PlatformInputEvent = InputEvent

actual typealias PlatformKeyEvent = KeyEvent

actual typealias PlatformMotionEvent = MotionEvent

actual val PlatformInputEvent.deviceId: Int get() = deviceId

actual val PlatformInputEvent.device: PlatformInputDevice? get() = device

actual val PlatformInputEvent.source: Int get() = source

actual val PlatformInputEvent.eventTime: Long get() = eventTime

actual fun PlatformInputEvent.isFromSource(source: Int): Boolean = isFromSource(source)

actual val PlatformKeyEvent.keyCode: Int get() = keyCode

actual val PlatformKeyEvent.action: Int get() = action

actual val PlatformKeyEvent.repeatCount: Int get() = repeatCount

actual val PlatformKeyEvent.scanCode: Int get() = scanCode

actual val PlatformKeyEvent.metaState: Int get() = metaState

actual val PlatformKeyEvent.downTime: Long get() = downTime

actual val PlatformMotionEvent.action: Int get() = action

actual val PlatformMotionEvent.actionMasked: Int get() = actionMasked

actual fun PlatformMotionEvent.getAxisValue(axis: Int): Float = getAxisValue(axis)
