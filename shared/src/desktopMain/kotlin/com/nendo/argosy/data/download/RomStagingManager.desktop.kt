package com.nendo.argosy.data.download

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class RomStagingManager @Inject constructor() {
    actual val stagingRoot: File = TODO("desktop: stagingRoot")

    actual fun claim(downloadId: Long): Boolean = false

    actual fun internalAvailableBytes(): Long? = null

    actual fun availableBytes(dir: File): Long? = null

    actual fun open(manifest: StagingManifest): StagingArea = TODO("desktop: open")

    actual fun advance(area: StagingArea, phase: StagingPhase, launchRelPath: String?): StagingArea = TODO("desktop: advance")

    actual fun discard(area: StagingArea) {
    }

    actual fun list(): List<StagingArea> = emptyList()

    actual fun cleanAbandoned(liveDownloadIds: Set<Long>): Long = 0L

    actual fun stagedBytes(): Long = 0L

    actual fun outputBytes(area: StagingArea): Long = 0L

    actual suspend fun deploy(area: StagingArea, onProgress: (copiedBytes: Long, totalBytes: Long) -> Unit): Boolean = false

    actual fun release(downloadId: Long) {
    }
}
