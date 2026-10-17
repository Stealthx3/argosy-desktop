package com.nendo.argosy.data.media

import com.nendo.argosy.data.storage.FileAccessLayer

expect object MediaSubtitleSidecars {
    fun pathFor(videoPath: String, streamIndex: Int, language: String?, format: String): String

    fun listFor(videoPath: String, fileAccessLayer: FileAccessLayer): List<MediaSubtitleSidecar>

    fun deleteAllFor(videoPath: String, fileAccessLayer: FileAccessLayer)

    fun bytesFor(videoPath: String, fileAccessLayer: FileAccessLayer): Long
}
