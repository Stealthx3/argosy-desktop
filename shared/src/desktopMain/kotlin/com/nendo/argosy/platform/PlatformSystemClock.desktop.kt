package com.nendo.argosy.platform

actual object PlatformSystemClock {
    actual fun elapsedRealtime(): Long = System.nanoTime() / NANOS_PER_MILLI
    actual fun uptimeMillis(): Long = System.nanoTime() / NANOS_PER_MILLI

    private const val NANOS_PER_MILLI = 1_000_000L
}
