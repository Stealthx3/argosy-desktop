package com.nendo.argosy.platform

actual class PlatformLooper

actual object PlatformLoopers {
    private val main = PlatformLooper()

    actual fun getMainLooper(): PlatformLooper = main
}

actual open class PlatformHandler actual constructor(looper: PlatformLooper)

actual abstract class PlatformContentObserver actual constructor(handler: PlatformHandler?) {
    actual open fun onChange(selfChange: Boolean) {
    }
}

actual fun PlatformContentResolver.registerContentObserver(
    uri: PlatformUri,
    notifyForDescendants: Boolean,
    observer: PlatformContentObserver,
) {
}

actual fun PlatformContentResolver.unregisterContentObserver(observer: PlatformContentObserver) {
}
