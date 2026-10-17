package com.nendo.argosy.data.sync.platform

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.data.emulator.SavePathRegistry
import com.nendo.argosy.data.storage.FileAccessLayer
import com.nendo.argosy.util.Logger
import com.nendo.argosy.di.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetroArchSaveHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fal: FileAccessLayer,
    private val retroArchPathResolver: com.nendo.argosy.data.emulator.RetroArchPathResolver,
) : PlatformSaveHandler {
    companion object {
        private const val TAG = "RetroArchSaveHandler"
        private val SAVE_EXTENSIONS = listOf("srm", "sav")
    }

    override suspend fun prepareForUpload(localPath: String, context: SaveContext): PreparedSave? =
        withContext(Dispatchers.IO) {
            val file = fal.getTransformedFile(localPath)
            if (!file.exists() || file.isDirectory) {
                Logger.debug(TAG, "prepareForUpload: Save file does not exist | path=$localPath")
                return@withContext null
            }

            // File-based: return as-is, no zipping needed
            Logger.debug(TAG, "prepareForUpload: Using file directly | path=$localPath, size=${file.length()}")
            PreparedSave(file, isTemporary = false, listOf(localPath))
        }

    override suspend fun extractDownload(tempFile: File, context: SaveContext): ExtractResult =
        withContext(Dispatchers.IO) {
            // Use cached path if available, otherwise discover/construct
            val targetPath = context.localSavePath
                ?: discoverSavePath(context)
                ?: constructSavePath(context)

            if (targetPath == null) {
                return@withContext ExtractResult(false, null, "Cannot resolve RetroArch save path")
            }

            val parentDir = File(targetPath).parent
            if (parentDir != null) fal.mkdirs(parentDir)

            val success = fal.copyFile(tempFile.absolutePath, targetPath)
            if (!success) {
                Logger.error(TAG, "extractDownload: Failed to copy file | target=$targetPath")
                return@withContext ExtractResult(false, null, "Failed to copy RetroArch save")
            }

            Logger.debug(TAG, "extractDownload: Complete | target=$targetPath")
            ExtractResult(true, targetPath)
        }

    suspend fun discoverSavePath(context: SaveContext): String? {
        val coreName = context.coreName ?: SavePathRegistry.getRetroArchCore(context.platformSlug)
        if (coreName == null) {
            Logger.debug(TAG, "discoverSavePath: No core mapping for platform | platform=${context.platformSlug}")
            return null
        }

        val req = com.nendo.argosy.data.emulator.RetroArchPathResolver.Request(
            emulatorId = context.emulatorId,
            coreName = coreName,
            romPath = context.romPath,
        )
        val paths = retroArchPathResolver.resolveSaveDirectories(req)

        val baseName = buildSaveFileName(context)

        for (basePath in paths) {
            for (ext in SAVE_EXTENSIONS) {
                val candidate = "$basePath/$baseName.$ext"
                if (fal.exists(candidate)) {
                    Logger.debug(TAG, "discoverSavePath: Found save | path=$candidate")
                    return candidate
                }
            }
        }

        Logger.debug(TAG, "discoverSavePath: No save found | baseName=$baseName, pathsChecked=${paths.size}")
        return null
    }

    suspend fun constructSavePath(context: SaveContext): String? {
        val coreName = context.coreName ?: SavePathRegistry.getRetroArchCore(context.platformSlug)
        if (coreName == null) {
            Logger.debug(TAG, "constructSavePath: No core mapping for platform | platform=${context.platformSlug}")
            return null
        }

        val req = com.nendo.argosy.data.emulator.RetroArchPathResolver.Request(
            emulatorId = context.emulatorId,
            coreName = coreName,
            romPath = context.romPath,
        )
        val baseName = buildSaveFileName(context)
        val saveExtension = context.config.saveExtensions.firstOrNull() ?: "srm"
        val savePath = retroArchPathResolver.buildSaveFilePath(req, baseName, saveExtension)
        Logger.debug(TAG, "constructSavePath: Constructed | path=$savePath")
        return savePath
    }

    private fun buildSaveFileName(context: SaveContext): String {
        // RetroArch uses ROM filename (without extension) as save filename
        if (context.romPath != null) {
            return File(context.romPath).nameWithoutExtension
        }
        // Fallback: sanitize game title
        return context.gameTitle.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}
