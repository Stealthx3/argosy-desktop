package com.nendo.argosy.data.repository

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class GameDocumentLoader @Inject constructor() {
    actual suspend fun readPdf(localPath: String?, rommFileId: Long?, remoteUrl: String?, fileName: String, pageWidthPx: Int): DocumentContent = TODO("desktop: readPdf")

    actual suspend fun cacheDocument(rommFileId: Long?, remoteUrl: String?, fileName: String): File? = null

    actual fun isCached(rommFileId: Long?, remoteUrl: String?, fileName: String): Boolean = false

    actual suspend fun readText(localPath: String?, rommFileId: Long?, remoteUrl: String?, fileName: String, maxBytes: Int): DocumentContent = TODO("desktop: readText")
}
