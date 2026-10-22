package com.nendo.argosy.data.music

import com.nendo.argosy.data.preferences.StoragePreferencesRepository
import com.nendo.argosy.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MusicDirectoryManager"

@Singleton
actual class MusicDirectoryManager @Inject constructor(
    private val storagePreferences: StoragePreferencesRepository
) {

    actual suspend fun resolveMusicDir(): File {
        val override = storagePreferences.preferences.first().musicStoragePath
        return if (override != null) File(override) else defaultMusicDir()
    }

    actual fun defaultMusicDir(): File {
        val music = System.getenv("XDG_MUSIC_DIR")?.takeIf { it.isNotBlank() }?.let(::File)
            ?: File(System.getProperty("user.home"), "Music")
        return File(music, "RomM")
    }

    actual suspend fun targetFileFor(
        platformName: String,
        gameName: String,
        trackNumber: Int?,
        title: String?,
        fileName: String
    ): File = targetFileIn(resolveMusicDir(), platformName, gameName, trackNumber, title, fileName)

    actual fun targetFileIn(
        musicDir: File,
        platformName: String,
        gameName: String,
        trackNumber: Int?,
        title: String?,
        fileName: String
    ): File {
        val extension = fileName.substringAfterLast('.', "")
        val baseTitle = sanitize(title ?: fileName.substringBeforeLast('.'))
        val baseName = if (trackNumber != null) {
            String.format(Locale.ROOT, "%02d - %s", trackNumber, baseTitle)
        } else {
            baseTitle
        }
        val targetName = if (extension.isNotEmpty()) "$baseName.$extension" else baseName
        val gameDir = File(File(musicDir, sanitize(platformName)), sanitize(gameName))
        return File(gameDir, targetName)
    }

    actual fun moveIntoMusic(source: File, target: File): Boolean {
        return try {
            target.parentFile?.mkdirs()
            if (target.exists()) target.delete()
            if (source.renameTo(target)) {
                true
            } else {
                source.copyTo(target, overwrite = true)
                source.delete()
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    actual suspend fun countFiles(): Int = withContext(Dispatchers.IO) {
        val dir = resolveMusicDir()
        if (dir.exists()) dir.walkTopDown().count { it.isFile } else 0
    }

    actual suspend fun relocate(source: File, destination: File): Unit = withContext(Dispatchers.IO) {
        if (!source.exists() || source.absolutePath == destination.absolutePath) return@withContext
        moveTree(source, destination)
    }

    private fun moveTree(source: File, destination: File) {
        destination.mkdirs()
        val files = source.listFiles() ?: return
        for (file in files) {
            val target = File(destination, file.name)
            try {
                if (file.isDirectory) {
                    moveTree(file, target)
                } else {
                    if (target.exists()) target.delete()
                    if (!file.renameTo(target)) {
                        file.copyTo(target, overwrite = true)
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                Logger.warn(TAG, "Failed to move ${file.name}: ${e.message}")
            }
        }
        if (source.listFiles()?.isEmpty() == true) source.delete()
    }

    actual fun scanFile(file: File) {
    }

    private fun sanitize(name: String): String =
        name.replace(INVALID_CHARS, "_").trim().ifEmpty { "track" }

    actual companion object {
        private val INVALID_CHARS = Regex("[\\\\/:*?\"<>|;=]")
    }
}
