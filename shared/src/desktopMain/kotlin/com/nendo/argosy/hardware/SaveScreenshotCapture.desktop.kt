package com.nendo.argosy.hardware

import com.nendo.argosy.platform.PlatformBitmap as Bitmap
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class SaveScreenshotCapture @Inject constructor() {
    actual fun fileFor(gameId: Long): File = TODO("desktop: fileFor")

    actual fun recentFor(gameId: Long, maxAgeMs: Long): File? = null

    actual suspend fun capture(gameId: Long, displayId: Int): Boolean = false

    actual fun store(gameId: Long, frame: Bitmap): Boolean = false

    actual fun snapshotThumbFor(snapshotId: Long): File? = null

    actual fun keepForSnapshot(snapshotId: Long, frame: File): Boolean = false

    actual fun carrySnapshotThumb(fromSnapshotId: Long, toSnapshotId: Long) {
    }
}
