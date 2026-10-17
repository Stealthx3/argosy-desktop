package com.nendo.argosy.platform

expect object PlatformSystemClock {
    fun elapsedRealtime(): Long
    fun uptimeMillis(): Long
}
