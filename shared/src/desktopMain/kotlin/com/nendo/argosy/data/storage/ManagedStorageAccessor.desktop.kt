package com.nendo.argosy.data.storage

import com.nendo.argosy.platform.PlatformUri as Uri
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class ManagedStorageAccessor @Inject constructor() {
    actual companion object {}

    actual data class DocumentFile(
        actual val documentId: String,
        actual val displayName: String,
        actual val mimeType: String,
        actual val lastModified: Long,
        actual val size: Long,
        actual val flags: Int,
        actual val isDirectory: Boolean
    ) {
        actual val uri: Uri
            get() = TODO("desktop")
    }

    actual fun listAndroidDataFiles(packageName: String, subPath: String?, volumeId: String): List<DocumentFile>? = null

    actual fun listFiles(volumeId: String, relativePath: String): List<DocumentFile>? = null

    actual fun isListableDirectory(volumeId: String, relativePath: String): Boolean = false

    actual fun exists(packageName: String, subPath: String?, volumeId: String): Boolean = false

    actual fun existsAtPath(volumeId: String, relativePath: String): Boolean = false

    actual fun isDirectoryAtPath(volumeId: String, relativePath: String): Boolean = false

    actual fun openInputStream(packageName: String, filePath: String, volumeId: String): InputStream? = null

    actual fun openInputStreamAtPath(volumeId: String, relativePath: String): InputStream? = null

    actual fun openOutputStream(packageName: String, filePath: String, volumeId: String): OutputStream? = null

    actual fun openOutputStreamAtPath(volumeId: String, relativePath: String): OutputStream? = null

    actual fun createDirectoryAtPath(volumeId: String, relativePath: String): Boolean = false

    actual fun delete(packageName: String, filePath: String, volumeId: String): Boolean = false

    actual fun deleteAtPath(volumeId: String, relativePath: String): Boolean = false

    actual fun copyFile(sourcePackage: String, sourcePath: String, destPackage: String, destPath: String, volumeId: String): Boolean = false

    actual fun getAbsolutePath(packageName: String, subPath: String?, volumeId: String): String = ""

    actual fun isManagedAccessSupported(): Boolean = false
}
