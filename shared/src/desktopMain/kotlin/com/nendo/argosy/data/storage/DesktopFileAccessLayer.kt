package com.nendo.argosy.data.storage

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import javax.inject.Inject

class DesktopFileAccessLayer @Inject constructor() : FileAccessLayer {

    override fun exists(path: String): Boolean = File(path).exists()

    override fun isDirectory(path: String): Boolean = File(path).isDirectory

    override fun isFile(path: String): Boolean = File(path).isFile

    override fun length(path: String): Long = File(path).length()

    override fun lastModified(path: String): Long = File(path).lastModified()

    override fun canRead(path: String): Boolean = File(path).canRead()

    override fun canWrite(path: String): Boolean = File(path).canWrite()

    override fun listFiles(path: String): List<FileInfo>? = File(path).listFiles()?.map { it.toFileInfo() }

    override fun listFilesUnion(path: String): List<FileInfo> = listFiles(path).orEmpty()

    override fun isListable(path: String): Boolean = File(path).list() != null

    override fun mkdirs(path: String): Boolean = File(path).let { it.isDirectory || it.mkdirs() }

    override fun delete(path: String): Boolean = File(path).delete()

    override fun deleteRecursively(path: String): Boolean = File(path).deleteRecursively()

    override fun readBytes(path: String): ByteArray? = runCatching { File(path).readBytes() }.getOrNull()

    override fun writeBytes(path: String, data: ByteArray): Boolean = runCatching {
        File(path).apply { parentFile?.mkdirs() }.writeBytes(data)
    }.isSuccess

    override fun getInputStream(path: String): InputStream? = runCatching { File(path).inputStream() }.getOrNull()

    override fun getOutputStream(path: String): OutputStream? = runCatching {
        File(path).apply { parentFile?.mkdirs() }.outputStream()
    }.getOrNull()

    override fun openSeekable(path: String, writable: Boolean): SeekableFile? = runCatching {
        val file = RandomAccessFile(path, if (writable) "rw" else "r")
        object : SeekableFile {
            override val size: Long get() = file.length()

            override fun read(position: Long, buffer: ByteArray, offset: Int, length: Int): Int {
                file.seek(position)
                return file.read(buffer, offset, length)
            }

            override fun write(position: Long, buffer: ByteArray, offset: Int, length: Int) {
                file.seek(position)
                file.write(buffer, offset, length)
            }

            override fun close() = file.close()
        }
    }.getOrNull()

    override fun copyFile(source: String, dest: String): Boolean = runCatching {
        File(source).copyTo(File(dest), overwrite = true)
    }.isSuccess

    override fun copyDirectory(source: String, dest: String): Boolean = runCatching {
        File(source).copyRecursively(File(dest), overwrite = true)
    }.getOrDefault(false)

    override fun walk(path: String): Sequence<FileInfo> = File(path).walkTopDown().map { it.toFileInfo() }

    override fun isRestrictedPath(path: String): Boolean = false

    override fun normalizeForDisplay(path: String): String = path

    override fun externalStorageRoots(): List<String> = listOf(System.getProperty("user.home"))

    override fun getTransformedFile(path: String): File = File(path)

    override fun prepareSaveAccess(vararg paths: String?) = Unit

    override fun commitSaveAccess(vararg paths: String?): Boolean = true

    private fun File.toFileInfo() = FileInfo(
        path = absolutePath,
        name = name,
        isDirectory = isDirectory,
        isFile = isFile,
        size = length(),
        lastModified = lastModified(),
    )
}
