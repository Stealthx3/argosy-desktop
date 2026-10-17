package com.nendo.argosy.hardware

import com.nendo.argosy.platform.PlatformBitmap as Bitmap
import java.io.File

expect class SaveScreenshotCapture {
    fun fileFor(gameId: Long): File

    fun recentFor(gameId: Long, maxAgeMs: Long = 6 * 60 * 60 * 1000L): File?

    suspend fun capture(gameId: Long, displayId: Int): Boolean

    fun store(gameId: Long, frame: Bitmap): Boolean

    fun snapshotThumbFor(snapshotId: Long): File?

    fun keepForSnapshot(snapshotId: Long, frame: File): Boolean

    fun carrySnapshotThumb(fromSnapshotId: Long, toSnapshotId: Long)
}
