package com.nendo.argosy.data.cache

import com.nendo.argosy.data.apps.HostApplications
import com.nendo.argosy.data.apps.HostIcons
import com.nendo.argosy.data.local.dao.GameArtDao
import com.nendo.argosy.data.model.ArtSlot
import com.nendo.argosy.platform.DesktopDirectories
import com.nendo.argosy.data.model.ResolvedGameArt
import com.nendo.argosy.platform.HostShell
import com.nendo.argosy.util.PlatformLog as Log
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Singleton
actual class ImageCacheManager @Inject constructor(
    private val gameArtDao: GameArtDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    actual val gameArt: StateFlow<Map<Long, ResolvedGameArt>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap())

    actual fun artFor(gameId: Long): ResolvedGameArt? = null

    actual suspend fun loadArt(gameId: Long): ResolvedGameArt = ResolvedGameArt()

    actual fun observeArt(gameId: Long): Flow<ResolvedGameArt?> = kotlinx.coroutines.flow.flowOf(null)

    actual fun setCustomCachePath(path: String?) {
    }

    actual fun getCustomCachePath(): String? = null

    actual fun getDefaultCachePath(): String = ""

    actual fun getCurrentCachePath(): String = ""

    actual val progress: kotlinx.coroutines.flow.StateFlow<ImageCacheProgress> = kotlinx.coroutines.flow.MutableStateFlow(ImageCacheProgress())

    actual val screenshotProgress: kotlinx.coroutines.flow.StateFlow<ImageCacheProgress> = kotlinx.coroutines.flow.MutableStateFlow(ImageCacheProgress())

    actual fun pauseBackgroundCaching() {
    }

    actual fun resumeBackgroundCaching() {
    }

    actual fun queueBackgroundCache(url: String, rommId: Long, gameTitle: String) {
    }

    actual fun queueBackgroundCache(urls: List<String>, rommId: Long, gameTitle: String) {
    }

    actual fun queueSteamBackgroundCache(url: String, steamAppId: Long, gameTitle: String) {
    }

    actual fun queueBackgroundCacheByGameId(url: String, gameId: Long, gameTitle: String) {
    }

    actual fun isLikelyAppIcon(file: File): Boolean = false

    actual suspend fun clearPlatformCache(platformSlug: String): Long = 0L

    actual suspend fun clearCache() {
    }

    actual suspend fun clearDecodedImageCache() {
    }

    actual suspend fun deleteGameImages(rommId: Long, clearDecoded: Boolean) {
    }

    actual suspend fun forgetCachedArt(gameId: Long) {
    }

    actual suspend fun forgetCachedArt(gameId: Long, slot: ArtSlot) {
    }

    actual fun getCacheSize(): Long = 0L

    actual fun getCacheSizeForBasePath(basePath: String): Long = 0L

    actual fun getCacheFileCount(): Int = 0

    actual fun getCacheFileCountForBasePath(basePath: String): Int = 0

    actual suspend fun migrateCache(fromBasePath: String, toBasePath: String, onProgress: (Int, Int) -> Unit): Boolean = false

    actual fun needsLegacyCacheDirsMigration(): Boolean = false

    actual suspend fun migrateLegacyCacheDirs() {
    }

    actual fun getPendingCount(): Int = 0

    actual fun resumePendingArt(includeBoxFaces: Boolean) {
    }

    actual suspend fun queueArtIfStale(gameId: Long, slot: ArtSlot, urls: List<String>, rommId: Long?, steamAppId: Long?, title: String) {
    }

    actual fun repairMissingArt(gameId: Long, slot: ArtSlot) {
    }

    actual fun queueScreenshotCache(gameId: Long, rommId: Long, gameTitle: String) {
    }

    actual fun queueScreenshotCacheByGameId(gameId: Long) {
    }

    actual suspend fun cacheSingleScreenshot(gameId: Long, url: String, index: Int): String? = null

    actual suspend fun userScreenshotTargetFile(rommId: Long, screenshotId: Long, version: String): File =
        DesktopDirectories.cache.resolve("screenshots").resolve(rommId.toString())
            .also { it.mkdirs() }
            .resolve("${screenshotId}_$version")

    actual fun pruneStaleUserScreenshots(rommId: Long, screenshotId: Long, keep: File) {
    }

    actual fun resumePendingScreenshotCache() {
    }

    actual fun queuePlatformLogoCache(platformId: Long, logoUrl: String) {
    }

    actual fun resumePendingLogoCache() {
    }

    actual enum class BoxFace { BACK, SPINE, LOGO, BOX_3D }

    actual fun queueCoverCache(url: String, rommId: Long, gameTitle: String) {
    }

    actual fun queueCoverCache(urls: List<String>, rommId: Long, gameTitle: String) {
    }

    actual fun queueCoverRevalidation(cachedPath: String, urls: List<String>, rommId: Long, gameTitle: String) {
    }

    actual fun queueBackgroundRevalidation(cachedPath: String, urls: List<String>, rommId: Long, gameTitle: String) {
    }

    actual fun queueBoxFaceCache(urls: List<String>, rommId: Long, gameTitle: String, face: BoxFace) {
    }

    actual fun queueCoverCacheByGameId(url: String, gameId: Long) {
    }

    actual fun queueCoverCacheByGameId(urls: List<String>, gameId: Long) {
    }

    actual suspend fun cacheGameImagesNow(rommId: Long, gameTitle: String, coverUrls: List<String>, backgroundUrls: List<String>, boxBackUrls: List<String>, boxSpineUrls: List<String>, logoUrls: List<String>, box3dUrls: List<String>): CachedGameImages = CachedGameImages(coverPath = null, backgroundPath = null, logoPath = null)

    actual suspend fun applyArtOverride(gameId: Long, slot: ArtSlot, url: String): Boolean = false

    actual suspend fun applyArtOverrideFromFile(gameId: Long, slot: ArtSlot, path: String): Boolean = false

    actual suspend fun clearArtOverride(gameId: Long, slot: ArtSlot) {
    }

    actual fun queueBadgeCache(achievementId: Long, badgeUrl: String, badgeUrlLock: String?) {
    }

    actual fun resumePendingBadgeCache() {
    }

    actual suspend fun setScreenshotAsBackground(gameId: Long, screenshotPath: String): Boolean = false

    actual fun queueAppIconCache(gameId: Long, packageName: String) {
        scope.launch {
            val path = runCatching { cacheAppIconSync(packageName) }
                .onFailure { Log.e(TAG, "Failed to cache app icon for $packageName: ${it.message}") }
                .getOrNull() ?: return@launch
            gameArtDao.setCached(gameId, ArtSlot.COVER, path, null)
        }
    }

    actual suspend fun cacheAppIconSync(packageName: String): String? = withContext(Dispatchers.IO) {
        val icon = HostApplications.find(packageName)?.icon ?: return@withContext null
        val source = HostIcons.pathFor(icon) ?: return@withContext null
        val extension = if (source.endsWith(".svg")) "svg" else "png"
        val cached = DesktopDirectories.cache.resolve("icons").also { it.mkdirs() }
            .resolve("appicon_${packageName.hashCode()}.$extension")
        if (cached.length() == 0L) {
            val bytes = HostShell.readBytes(source)?.takeIf { it.isNotEmpty() } ?: return@withContext null
            cached.writeBytes(bytes)
        }
        cached.absolutePath
    }

    actual suspend fun validateAndCleanCache(force: Boolean, onProgress: (suspend (phase: String, current: Int, total: Int) -> Unit)?): CacheValidationResult = CacheValidationResult(deletedFiles = 0, clearedPaths = 0)

    actual fun needsFlatToShardedMigration(): Boolean = false

    actual suspend fun migrateFlatToSharded(onProgress: (suspend (current: Int, total: Int) -> Unit)?) {
    }

    actual companion object {
        private const val TAG = "ImageCacheManager"
    }
}
