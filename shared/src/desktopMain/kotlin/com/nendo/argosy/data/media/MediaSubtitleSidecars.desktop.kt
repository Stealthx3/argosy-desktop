package com.nendo.argosy.data.media

import com.nendo.argosy.data.storage.FileAccessLayer

actual object MediaSubtitleSidecars {
    actual fun pathFor(videoPath: String, streamIndex: Int, language: String?, format: String): String = ""

    actual fun listFor(videoPath: String, fileAccessLayer: FileAccessLayer): List<MediaSubtitleSidecar> = emptyList()

    actual fun deleteAllFor(videoPath: String, fileAccessLayer: FileAccessLayer) {
    }

    actual fun bytesFor(videoPath: String, fileAccessLayer: FileAccessLayer): Long = 0L
}
