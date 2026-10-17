package com.nendo.argosy.libretro

expect class CoreCrashDetector {
    suspend fun detect(): CoreCrashInfo?
}
