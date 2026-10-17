package com.nendo.argosy.data.repository
import com.nendo.argosy.platform.asImageBitmap
import com.nendo.argosy.platform.config
import com.nendo.argosy.platform.copy
import com.nendo.argosy.platform.getPixel
import com.nendo.argosy.platform.height
import com.nendo.argosy.platform.recycle
import com.nendo.argosy.platform.setPixels
import com.nendo.argosy.platform.width

sealed interface DocumentContent {
    data class Text(val body: String) : DocumentContent
    data class Pages(val pages: List<com.nendo.argosy.platform.PlatformBitmap>) : DocumentContent
    data class Unavailable(val reason: String?) : DocumentContent
}

/**
 * The stable name a document is cached and annotated under: its RomM file id, else a hash of its
 * url. Null for a document with neither.
 */
fun documentKey(rommFileId: Long?, remoteUrl: String?): String? =
    rommFileId?.let { "romm_$it" } ?: remoteUrl?.let { "url_${it.hashCode().toUInt()}" }
