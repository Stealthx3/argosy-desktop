package com.nendo.argosy.data.download

import java.io.File

expect class RomStagingManager {
    val stagingRoot: File

    fun claim(downloadId: Long): Boolean

    fun release(downloadId: Long)

    fun internalAvailableBytes(): Long?

    fun availableBytes(dir: File): Long?

    fun open(manifest: StagingManifest): StagingArea

    fun advance(area: StagingArea, phase: StagingPhase, launchRelPath: String? = null): StagingArea

    fun discard(area: StagingArea)

    fun list(): List<StagingArea>

    fun cleanAbandoned(liveDownloadIds: Set<Long>): Long

    fun stagedBytes(): Long

    fun outputBytes(area: StagingArea): Long

    suspend fun deploy(area: StagingArea, onProgress: (copiedBytes: Long, totalBytes: Long) -> Unit): Boolean
}
