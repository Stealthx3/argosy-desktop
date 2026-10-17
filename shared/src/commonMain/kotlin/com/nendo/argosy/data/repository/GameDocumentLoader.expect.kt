package com.nendo.argosy.data.repository

import java.io.File

expect class GameDocumentLoader {
    suspend fun readPdf(localPath: String?, rommFileId: Long?, remoteUrl: String?, fileName: String, pageWidthPx: Int): DocumentContent

    suspend fun cacheDocument(rommFileId: Long?, remoteUrl: String?, fileName: String): File?

    fun isCached(rommFileId: Long?, remoteUrl: String?, fileName: String): Boolean

    suspend fun readText(localPath: String?, rommFileId: Long?, remoteUrl: String? = null, fileName: String, maxBytes: Int = 4 * 1024 * 1024): DocumentContent
}
