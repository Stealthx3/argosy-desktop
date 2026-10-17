package com.nendo.argosy.data.emulator

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class ScreenshotCaptureMonitor @Inject constructor() {
    actual companion object {}

    actual fun start(gameId: Long, emulatorPackage: String?, sessionStartTime: Long) {
    }

    actual fun stop() {
    }
}
