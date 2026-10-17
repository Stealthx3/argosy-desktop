package com.nendo.argosy.data.emulator

expect class ScreenshotCaptureMonitor {
    companion object {}

    fun start(gameId: Long, emulatorPackage: String?, sessionStartTime: Long)

    fun stop()
}
