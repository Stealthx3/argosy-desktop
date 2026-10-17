package com.nendo.argosy.data.storage

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class AndroidDataAccessor @Inject constructor() {
    actual fun resetAltAccessCache() {
    }

    actual fun isAltAccessSupported(): Boolean = false

    actual fun getAllStorageRoots(): List<String> = emptyList()

    actual fun syncMirror(paths: Collection<String>): Boolean? = null

    actual fun transformPath(path: String): String = ""

    actual fun normalizePathForDisplay(path: String): String = ""

    actual fun isRestrictedAndroidPath(path: String): Boolean = false

    actual fun listFiles(path: String): Array<File>? = null

    actual fun exists(path: String): Boolean = false

    actual fun canRead(path: String): Boolean = false

    actual fun canWrite(path: String): Boolean = false

    actual fun readBytes(path: String): ByteArray? = null

    actual fun writeBytes(path: String, data: ByteArray): Boolean = false

    actual fun delete(path: String): Boolean = false

    actual fun deleteRecursively(path: String): Boolean = false

    actual fun getFile(path: String): File = TODO("desktop: getFile")

    actual fun getInputStream(path: String): InputStream? = null

    actual fun getOutputStream(path: String): OutputStream? = null

    actual fun lastModified(path: String): Long = 0L

    actual fun length(path: String): Long = 0L

    actual fun isDirectory(path: String): Boolean = false

    actual fun isFile(path: String): Boolean = false

    actual fun mkdirs(path: String): Boolean = false

    actual fun walk(path: String): Sequence<File> = TODO("desktop: walk")

    actual fun copyFile(sourcePath: String, destPath: String): Boolean = false

    actual fun copyDirectory(sourcePath: String, destPath: String): Boolean = false

    actual fun moveDirectory(sourcePath: String, destPath: String): Boolean = false
}
