package com.nendo.argosy.libretro.frame

import com.nendo.argosy.platform.PlatformBitmap as Bitmap
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class FrameRegistry @Inject constructor() {
    actual enum class Source { LIBRETRO, DUIMON, CUSTOM }

    actual data class ScreenRect(
        actual val left: Float,
        actual val top: Float,
        actual val right: Float,
        actual val bottom: Float
    )

    actual data class FrameEntry(
        actual val id: String,
        actual val displayName: String,
        actual val platforms: Set<String>,
        actual val githubPath: String,
        actual val source: Source = Source.LIBRETRO,
        actual val screenRect: ScreenRect? = null
    )

    actual companion object {
        actual const val CUSTOM_ID_PREFIX: String = "custom_"

        actual const val NO_FRAME_ID: String = "none"

        actual fun downloadUrl(entry: FrameEntry): String = ""
    }

    actual fun getInstalledIds(): Set<String> = emptySet()

    actual fun invalidateInstalledCache() {
    }

    actual fun getCatalogFrames(): List<FrameEntry> = emptyList()

    actual fun getFramesForPlatform(platformSlug: String): List<FrameEntry> = emptyList()

    actual fun getAllFrames(): List<FrameEntry> = emptyList()

    actual fun getCustomFrames(): List<FrameEntry> = emptyList()

    actual fun importCustomFrame(sourcePath: String): Result<FrameEntry> = kotlin.Result.failure(UnsupportedOperationException("not available on desktop"))

    actual fun deleteCustomFrame(id: String): Boolean = false

    actual fun findById(id: String): FrameEntry? = null

    actual fun isInstalled(entry: FrameEntry): Boolean = false

    actual fun isInstalled(id: String): Boolean = false

    actual fun getInstalledFramesForPlatform(platformSlug: String): List<FrameEntry> = emptyList()

    actual fun loadFrame(id: String, maxWidth: Int, maxHeight: Int): Bitmap? = null

    actual fun loadFrame(entry: FrameEntry): Bitmap? = null

    actual fun getFramesDir(): File = TODO("desktop: getFramesDir")

    actual fun getCustomFramesDir(): File = TODO("desktop: getCustomFramesDir")

    actual fun installedFileFor(entry: FrameEntry): File = TODO("desktop: installedFileFor")

    actual fun ensureDirectoryExists() {
    }

    actual fun clearDownloadedFrames() {
    }
}
