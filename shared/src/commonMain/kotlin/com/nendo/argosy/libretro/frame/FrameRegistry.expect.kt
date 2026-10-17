package com.nendo.argosy.libretro.frame

import com.nendo.argosy.platform.PlatformBitmap as Bitmap
import java.io.File

expect class FrameRegistry {
    enum class Source { LIBRETRO, DUIMON, CUSTOM }

    class ScreenRect {
        val left: Float
        val top: Float
        val right: Float
        val bottom: Float
    }

    class FrameEntry {
        val id: String
        val displayName: String
        val platforms: Set<String>
        val githubPath: String
        val source: Source
        val screenRect: ScreenRect?
    }

    companion object {
        val CUSTOM_ID_PREFIX: String

        val NO_FRAME_ID: String

        fun downloadUrl(entry: FrameEntry): String
    }

    fun getInstalledIds(): Set<String>

    fun invalidateInstalledCache()

    fun getCatalogFrames(): List<FrameEntry>

    fun getFramesForPlatform(platformSlug: String): List<FrameEntry>

    fun getAllFrames(): List<FrameEntry>

    fun getCustomFrames(): List<FrameEntry>

    fun importCustomFrame(sourcePath: String): Result<FrameEntry>

    fun deleteCustomFrame(id: String): Boolean

    fun findById(id: String): FrameEntry?

    fun isInstalled(entry: FrameEntry): Boolean

    fun isInstalled(id: String): Boolean

    fun getInstalledFramesForPlatform(platformSlug: String): List<FrameEntry>

    fun loadFrame(id: String, maxWidth: Int = 0, maxHeight: Int = 0): Bitmap?

    fun loadFrame(entry: FrameEntry): Bitmap?

    fun getFramesDir(): File

    fun getCustomFramesDir(): File

    fun installedFileFor(entry: FrameEntry): File

    fun ensureDirectoryExists()

    fun clearDownloadedFrames()
}
