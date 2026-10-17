package com.nendo.argosy.hardware

import com.nendo.argosy.platform.PlatformBitmap as Bitmap
import com.nendo.argosy.platform.asImageBitmap
import com.nendo.argosy.platform.config
import com.nendo.argosy.platform.copy
import com.nendo.argosy.platform.getPixel
import com.nendo.argosy.platform.height
import com.nendo.argosy.platform.recycle
import com.nendo.argosy.platform.setPixels
import com.nendo.argosy.platform.width
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
