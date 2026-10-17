package com.nendo.argosy.data.storage

import com.nendo.argosy.platform.PlatformUri as Uri
import java.io.InputStream
import java.io.OutputStream

expect class ManagedStorageAccessor {
    companion object {}

    class DocumentFile {
        val documentId: String
        val displayName: String
        val mimeType: String
        val lastModified: Long
        val size: Long
        val flags: Int
        val isDirectory: Boolean
        val uri: Uri
    }

    fun listAndroidDataFiles(packageName: String, subPath: String? = null, volumeId: String = "primary"): List<DocumentFile>?

    fun listFiles(volumeId: String, relativePath: String): List<DocumentFile>?

    fun isListableDirectory(volumeId: String, relativePath: String): Boolean

    fun exists(packageName: String, subPath: String? = null, volumeId: String = "primary"): Boolean

    fun existsAtPath(volumeId: String, relativePath: String): Boolean

    fun isDirectoryAtPath(volumeId: String, relativePath: String): Boolean

    fun openInputStream(packageName: String, filePath: String, volumeId: String = "primary"): InputStream?

    fun openInputStreamAtPath(volumeId: String, relativePath: String): InputStream?

    fun openOutputStream(packageName: String, filePath: String, volumeId: String = "primary"): OutputStream?

    fun openOutputStreamAtPath(volumeId: String, relativePath: String): OutputStream?

    fun createDirectoryAtPath(volumeId: String, relativePath: String): Boolean

    fun delete(packageName: String, filePath: String, volumeId: String = "primary"): Boolean

    fun deleteAtPath(volumeId: String, relativePath: String): Boolean

    fun copyFile(sourcePackage: String, sourcePath: String, destPackage: String, destPath: String, volumeId: String = "primary"): Boolean

    fun getAbsolutePath(packageName: String, subPath: String? = null, volumeId: String = "primary"): String

    fun isManagedAccessSupported(): Boolean
}
