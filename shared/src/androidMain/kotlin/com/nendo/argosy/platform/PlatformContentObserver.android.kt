package com.nendo.argosy.platform

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper

actual typealias PlatformLooper = Looper

actual object PlatformLoopers {
    actual fun getMainLooper(): PlatformLooper = Looper.getMainLooper()
}

actual typealias PlatformHandler = Handler

actual typealias PlatformContentObserver = ContentObserver

actual fun PlatformContentResolver.registerContentObserver(
    uri: PlatformUri,
    notifyForDescendants: Boolean,
    observer: PlatformContentObserver,
) = registerContentObserver(uri, notifyForDescendants, observer)

actual fun PlatformContentResolver.unregisterContentObserver(observer: PlatformContentObserver) =
    unregisterContentObserver(observer)
