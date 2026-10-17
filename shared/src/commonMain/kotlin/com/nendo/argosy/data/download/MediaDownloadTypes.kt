package com.nendo.argosy.data.download

import com.nendo.argosy.data.preferences.MediaDownloadQuality
import kotlinx.coroutines.flow.first

sealed class MediaDownloadState {
    data object Idle : MediaDownloadState()
    data class Preparing(val itemId: String, val itemName: String, val detail: String) : MediaDownloadState()
    data class Downloading(val itemId: String, val itemName: String, val progress: Float) : MediaDownloadState()
    data class Paused(
        val itemId: String,
        val itemName: String,
        val progress: Float,
        val reason: String? = null
    ) : MediaDownloadState()
    data class Completed(val itemId: String, val itemName: String, val path: String) : MediaDownloadState()
    data class Failed(val itemId: String, val itemName: String, val error: String) : MediaDownloadState()
}

/**
 * Whether this state is work the queue still owes the user. A finished or failed download is neither
 * queued nor in progress, and counting it as either is what leaves a failure reading as pending.
 */
val MediaDownloadState.isInFlight: Boolean
    get() = this is MediaDownloadState.Preparing ||
        this is MediaDownloadState.Downloading ||
        this is MediaDownloadState.Paused

data class MediaDownloadProgress(
    val itemId: String,
    val itemName: String,
    val seriesId: String? = null,
    val seriesName: String? = null,
    val posterUrl: String? = null,
    val quality: MediaDownloadQuality,
    val totalBytes: Long,
    val bytesDownloaded: Long,
    val state: MediaDownloadState,
    val bytesPerSecond: Long = 0L,
    val sizeIsEstimated: Boolean = false
) {
    val progress: Float get() = if (totalBytes > 0) (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    val displayTitle: String get() = if (seriesName.isNullOrBlank()) itemName else "$seriesName - $itemName"
}

/**
 * A batch's size at one quality. [isSourceSize] marks a figure taken from what the server reported
 * about the file rather than computed from a tier's bitrate, so a caller can say which it is showing.
 */
data class MediaSizeEstimate(
    val bytes: Long,
    val isSourceSize: Boolean
)

/**
 * What subtitles a batch can be expected to keep, as far as anything already negotiated shows.
 *
 * [anythingKnown] separates a batch with no subtitles from one nothing has described yet, which are
 * different answers: the first means there is nothing to say, the second means the rule has to be
 * stated without promising the titles have any.
 */
data class MediaSubtitleOutlook(
    val hasTextSubtitles: Boolean,
    val hasImageSubtitles: Boolean,
    val burnsInImageSubtitles: Boolean,
    val anythingKnown: Boolean
)

data class QueuedMediaDownload(
    val itemId: String,
    val itemName: String,
    val seriesId: String? = null,
    val seriesName: String? = null,
    val itemType: String,
    val quality: MediaDownloadQuality,
    val posterUrl: String? = null
)

/**
 * What a removal batch did. [unreachable] is the count kept because their storage is not mounted -
 * those are still downloaded, and the caller is expected to say so rather than report a clean sweep.
 */
data class MediaRemovalResult(
    val removed: Int,
    val unreachable: Int,
    val absent: Int
)
