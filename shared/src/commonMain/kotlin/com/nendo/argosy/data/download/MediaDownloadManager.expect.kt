package com.nendo.argosy.data.download

import com.nendo.argosy.data.preferences.MediaDownloadQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

expect class MediaDownloadManager {
    val downloadState: StateFlow<MediaDownloadState>

    val activeDownload: StateFlow<MediaDownloadProgress?>

    val downloadQueue: StateFlow<List<QueuedMediaDownload>>

    val completedDownloads: StateFlow<List<MediaDownloadProgress>>

    fun hasActiveMediaDownload(): Boolean

    fun hasBlockingDownloadState(): Boolean

    fun clearCompletedDownloads()

    fun removeFromCompleted(itemId: String)

    fun onDownloadSlotFreed()

    suspend fun estimateBatch(itemIds: List<String>): Map<MediaDownloadQuality, MediaSizeEstimate>

    suspend fun subtitleOutlook(itemIds: List<String>): MediaSubtitleOutlook

    suspend fun availableBytes(): Long

    suspend fun defaultQuality(): MediaDownloadQuality

    fun enqueue(itemId: String, quality: MediaDownloadQuality)

    fun enqueueAll(itemIds: List<String>, quality: MediaDownloadQuality)

    fun pauseActiveDownload()

    fun pauseSeries(seriesId: String)

    fun resumeSeries(seriesId: String)

    fun cancelSeries(seriesId: String)

    fun resumeDownload(itemId: String)

    fun cancelDownload(itemId: String)

    suspend fun removeDownload(itemId: String): Boolean

    suspend fun removeDownloads(itemIds: List<String>): MediaRemovalResult
}
