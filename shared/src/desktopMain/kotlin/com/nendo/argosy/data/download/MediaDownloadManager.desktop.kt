package com.nendo.argosy.data.download

import com.nendo.argosy.data.preferences.MediaDownloadQuality
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
actual class MediaDownloadManager @Inject constructor() {
    actual val downloadState: StateFlow<MediaDownloadState> = kotlinx.coroutines.flow.MutableStateFlow<MediaDownloadState>(MediaDownloadState.Idle)

    actual val activeDownload: StateFlow<MediaDownloadProgress?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual val downloadQueue: StateFlow<List<QueuedMediaDownload>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())

    actual val completedDownloads: StateFlow<List<MediaDownloadProgress>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())

    actual fun hasActiveMediaDownload(): Boolean = false

    actual fun hasBlockingDownloadState(): Boolean = false

    actual fun clearCompletedDownloads() {
    }

    actual fun removeFromCompleted(itemId: String) {
    }

    actual fun onDownloadSlotFreed() {
    }

    actual suspend fun estimateBatch(itemIds: List<String>): Map<MediaDownloadQuality, MediaSizeEstimate> = emptyMap()

    actual suspend fun subtitleOutlook(itemIds: List<String>): MediaSubtitleOutlook = MediaSubtitleOutlook(hasTextSubtitles = false, hasImageSubtitles = false, burnsInImageSubtitles = false, anythingKnown = false)

    actual suspend fun availableBytes(): Long = 0L

    actual suspend fun defaultQuality(): MediaDownloadQuality = TODO("desktop: defaultQuality")

    actual fun enqueue(itemId: String, quality: MediaDownloadQuality) {
    }

    actual fun enqueueAll(itemIds: List<String>, quality: MediaDownloadQuality) {
    }

    actual fun pauseActiveDownload() {
    }

    actual fun pauseSeries(seriesId: String) {
    }

    actual fun resumeSeries(seriesId: String) {
    }

    actual fun cancelSeries(seriesId: String) {
    }

    actual fun resumeDownload(itemId: String) {
    }

    actual fun cancelDownload(itemId: String) {
    }

    actual suspend fun removeDownload(itemId: String): Boolean = false

    actual suspend fun removeDownloads(itemIds: List<String>): MediaRemovalResult = MediaRemovalResult(removed = 0, unreachable = 0, absent = 0)
}
