package com.nendo.argosy.data.download

import java.io.File

internal const val ARCHIVE_SUBDIR = "archive"

internal const val OUTPUT_SUBDIR = "out"

enum class StagingPhase { DOWNLOADING, EXTRACTING, MOVING }

data class StagingManifest(
    val downloadId: Long,
    val gameId: Long,
    val gameTitle: String,
    val fileName: String,
    val destinationDir: String,
    val phase: StagingPhase,
    val launchRelPath: String? = null
)

class StagingArea(val root: File, val manifest: StagingManifest) {
    val archiveDir: File get() = File(root, ARCHIVE_SUBDIR)
    val outputDir: File get() = File(root, OUTPUT_SUBDIR)
    val destinationDir: File get() = File(manifest.destinationDir)
}
