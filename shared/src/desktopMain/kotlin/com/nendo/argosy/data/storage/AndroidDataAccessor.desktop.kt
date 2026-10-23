package com.nendo.argosy.data.storage

import com.nendo.argosy.platform.PlatformEnvironment as Environment
import com.nendo.argosy.util.Logger
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Linux has no restricted package folders, so every path is used as given and the alt-access
 * machinery reports itself unsupported; the file operations match the Android actual's.
 */
@Singleton
actual class AndroidDataAccessor @Inject constructor(
    private val volumeDetector: StorageVolumeDetector
) {
    actual companion object {
        private const val TAG = "AndroidDataAccessor"

        actual fun isOnInternalVolume(path: String): Boolean = true
    }

    actual fun resetAltAccessCache() {
    }

    actual fun isAltAccessSupported(): Boolean = false

    actual fun getAllStorageRoots(): List<String> {
        val roots = mutableListOf(Environment.getExternalStorageDirectory().absolutePath)
        volumeDetector.volumeRootPaths().forEach { if (it !in roots) roots.add(it) }
        return roots
    }

    actual fun syncMirror(paths: Collection<String>): Boolean? = null

    actual fun transformPath(path: String): String = path

    actual fun normalizePathForDisplay(path: String): String = path

    actual fun isRestrictedAndroidPath(path: String): Boolean = false

    actual fun listFiles(path: String): Array<File>? {
        val dir = File(path)
        return if (dir.exists() && dir.isDirectory) dir.listFiles() else null
    }

    actual fun exists(path: String): Boolean = File(path).exists()

    actual fun canRead(path: String): Boolean = File(path).canRead()

    actual fun canWrite(path: String): Boolean = File(path).canWrite()

    actual fun readBytes(path: String): ByteArray? {
        val file = File(path)
        return if (file.exists() && file.canRead()) {
            try { file.readBytes() } catch (e: Exception) { null }
        } else null
    }

    actual fun writeBytes(path: String, data: ByteArray): Boolean {
        return try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeBytes(data)
            true
        } catch (e: Exception) { false }
    }

    actual fun delete(path: String): Boolean =
        try { File(path).delete() } catch (e: Exception) { false }

    actual fun deleteRecursively(path: String): Boolean =
        try { File(path).deleteRecursively() } catch (e: Exception) { false }

    actual fun getFile(path: String): File = File(path)

    actual fun getInputStream(path: String): InputStream? {
        val file = File(path)
        return if (file.exists() && file.canRead()) {
            try { file.inputStream() } catch (e: Exception) { null }
        } else null
    }

    actual fun getOutputStream(path: String): OutputStream? {
        return try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.outputStream()
        } catch (e: Exception) {
            Logger.error(TAG, "getOutputStream failed | path=$path, error=${e.message}")
            null
        }
    }

    actual fun lastModified(path: String): Long = File(path).lastModified()

    actual fun length(path: String): Long = File(path).length()

    actual fun isDirectory(path: String): Boolean = File(path).isDirectory

    actual fun isFile(path: String): Boolean = File(path).isFile

    actual fun mkdirs(path: String): Boolean {
        val file = File(path)
        return try { file.mkdirs() || file.exists() } catch (e: Exception) { false }
    }

    actual fun walk(path: String): Sequence<File> = File(path).walkTopDown()

    actual fun copyFile(sourcePath: String, destPath: String): Boolean {
        return try {
            val destFile = File(destPath)
            destFile.parentFile?.mkdirs()
            File(sourcePath).copyTo(destFile, overwrite = true)
            true
        } catch (e: Exception) { false }
    }

    actual fun copyDirectory(sourcePath: String, destPath: String): Boolean =
        try { File(sourcePath).copyRecursively(File(destPath), overwrite = true) } catch (e: Exception) { false }

    actual fun moveDirectory(sourcePath: String, destPath: String): Boolean {
        val sourceDir = File(sourcePath)
        val destDir = File(destPath)

        return try {
            destDir.parentFile?.mkdirs()

            if (sourceDir.renameTo(destDir)) return true

            if (destDir.exists()) destDir.deleteRecursively()
            val copied = sourceDir.copyRecursively(destDir, overwrite = true)
            if (copied) {
                sourceDir.deleteRecursively()
                return true
            }
            destDir.deleteRecursively()
            sourceDir.deleteRecursively()
            false
        } catch (e: Exception) {
            Logger.error(TAG, "moveDirectory failed | dest=$destPath, error=${e.message}")
            runCatching { sourceDir.deleteRecursively() }
            false
        }
    }
}
