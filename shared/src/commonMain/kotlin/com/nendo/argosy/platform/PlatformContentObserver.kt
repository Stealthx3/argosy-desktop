package com.nendo.argosy.platform

expect class PlatformLooper

expect object PlatformLoopers {
    fun getMainLooper(): PlatformLooper
}

expect open class PlatformHandler(looper: PlatformLooper)

expect abstract class PlatformContentObserver(handler: PlatformHandler?) {
    open fun onChange(selfChange: Boolean)
}

expect fun PlatformContentResolver.registerContentObserver(
    uri: PlatformUri,
    notifyForDescendants: Boolean,
    observer: PlatformContentObserver,
)

expect fun PlatformContentResolver.unregisterContentObserver(observer: PlatformContentObserver)
