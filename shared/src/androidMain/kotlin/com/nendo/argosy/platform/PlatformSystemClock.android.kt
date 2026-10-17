package com.nendo.argosy.platform

import android.os.SystemClock

actual object PlatformSystemClock {
    actual fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
    actual fun uptimeMillis(): Long = SystemClock.uptimeMillis()
}
