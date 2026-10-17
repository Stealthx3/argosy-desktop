package com.nendo.argosy.data.media

/**
 * How one subtitle track is asked for and what the player parses it as.
 *
 * ASS keeps its own format rather than being converted, because conversion to WebVTT discards the
 * positioning and styling a quarter of this kind of library depends on. A track with no text form at
 * all - a picture subtitle - has no delivery and answers null.
 */
data class MediaSubtitleDelivery(val format: String, val mimeType: String)

/**
 * One subtitle file stored beside a downloaded video.
 */
data class MediaSubtitleSidecar(
    val path: String,
    val streamIndex: Int,
    val language: String?,
    val delivery: MediaSubtitleDelivery
)
