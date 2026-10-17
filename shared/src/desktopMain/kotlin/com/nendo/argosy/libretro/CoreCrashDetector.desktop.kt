package com.nendo.argosy.libretro

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class CoreCrashDetector @Inject constructor() {
    actual suspend fun detect(): CoreCrashInfo? = null
}
