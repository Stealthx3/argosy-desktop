package com.nendo.argosy.data.storage

import java.io.File
import java.io.InputStream
import java.io.OutputStream

expect class AndroidDataAccessor {
    companion object {
        fun isOnInternalVolume(path: String): Boolean
    }

    fun resetAltAccessCache()

    fun isAltAccessSupported(): Boolean

    fun getAllStorageRoots(): List<String>

    fun syncMirror(paths: Collection<String>): Boolean?

    fun transformPath(path: String): String

    fun normalizePathForDisplay(path: String): String

    fun isRestrictedAndroidPath(path: String): Boolean

    fun listFiles(path: String): Array<File>?

    fun exists(path: String): Boolean

    fun canRead(path: String): Boolean

    fun canWrite(path: String): Boolean

    fun readBytes(path: String): ByteArray?

    fun writeBytes(path: String, data: ByteArray): Boolean

    fun delete(path: String): Boolean

    fun deleteRecursively(path: String): Boolean

    fun getFile(path: String): File

    fun getInputStream(path: String): InputStream?

    fun getOutputStream(path: String): OutputStream?

    fun lastModified(path: String): Long

    fun length(path: String): Long

    fun isDirectory(path: String): Boolean

    fun isFile(path: String): Boolean

    fun mkdirs(path: String): Boolean

    fun walk(path: String): Sequence<File>

    fun copyFile(sourcePath: String, destPath: String): Boolean

    fun copyDirectory(sourcePath: String, destPath: String): Boolean

    fun moveDirectory(sourcePath: String, destPath: String): Boolean
}
