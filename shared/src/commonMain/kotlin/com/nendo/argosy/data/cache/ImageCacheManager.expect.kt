package com.nendo.argosy.data.cache

import com.nendo.argosy.data.model.ArtSlot
import com.nendo.argosy.data.model.ResolvedGameArt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

expect class ImageCacheManager {
    companion object {}

    enum class BoxFace { BACK, SPINE, LOGO, BOX_3D }

    val gameArt: StateFlow<Map<Long, ResolvedGameArt>>

    fun artFor(gameId: Long): ResolvedGameArt?

    suspend fun loadArt(gameId: Long): ResolvedGameArt

    fun observeArt(gameId: Long): Flow<ResolvedGameArt?>

    fun setCustomCachePath(path: String?)

    fun getCustomCachePath(): String?

    fun getDefaultCachePath(): String

    fun getCurrentCachePath(): String

    val progress: kotlinx.coroutines.flow.StateFlow<ImageCacheProgress>

    val screenshotProgress: kotlinx.coroutines.flow.StateFlow<ImageCacheProgress>

    fun pauseBackgroundCaching()

    fun resumeBackgroundCaching()

    fun queueBackgroundCache(url: String, rommId: Long, gameTitle: String = "")

    fun queueBackgroundCache(urls: List<String>, rommId: Long, gameTitle: String = "")

    fun queueSteamBackgroundCache(url: String, steamAppId: Long, gameTitle: String = "")

    fun queueBackgroundCacheByGameId(url: String, gameId: Long, gameTitle: String = "")

    fun isLikelyAppIcon(file: File): Boolean

    suspend fun clearPlatformCache(platformSlug: String): Long

    suspend fun clearCache()

    suspend fun clearDecodedImageCache()

    suspend fun deleteGameImages(rommId: Long, clearDecoded: Boolean = true)

    suspend fun forgetCachedArt(gameId: Long)

    suspend fun forgetCachedArt(gameId: Long, slot: ArtSlot)

    fun getCacheSize(): Long

    fun getCacheSizeForBasePath(basePath: String): Long

    fun getCacheFileCount(): Int

    fun getCacheFileCountForBasePath(basePath: String): Int

    suspend fun migrateCache(fromBasePath: String, toBasePath: String, onProgress: (Int, Int) -> Unit = { _, _ -> }): Boolean

    fun needsLegacyCacheDirsMigration(): Boolean

    suspend fun migrateLegacyCacheDirs()

    fun getPendingCount(): Int

    fun resumePendingArt(includeBoxFaces: Boolean)

    suspend fun queueArtIfStale(gameId: Long, slot: ArtSlot, urls: List<String>, rommId: Long?, steamAppId: Long?, title: String)

    fun repairMissingArt(gameId: Long, slot: ArtSlot)

    fun queueScreenshotCache(gameId: Long, rommId: Long, gameTitle: String)

    fun queueScreenshotCacheByGameId(gameId: Long)

    suspend fun cacheSingleScreenshot(gameId: Long, url: String, index: Int): String?

    suspend fun userScreenshotTargetFile(rommId: Long, screenshotId: Long, version: String): File

    fun pruneStaleUserScreenshots(rommId: Long, screenshotId: Long, keep: File)

    fun resumePendingScreenshotCache()

    fun queuePlatformLogoCache(platformId: Long, logoUrl: String)

    fun resumePendingLogoCache()

    fun queueCoverCache(url: String, rommId: Long, gameTitle: String = "")

    fun queueCoverCache(urls: List<String>, rommId: Long, gameTitle: String = "")

    fun queueCoverRevalidation(cachedPath: String, urls: List<String>, rommId: Long, gameTitle: String = "")

    fun queueBackgroundRevalidation(cachedPath: String, urls: List<String>, rommId: Long, gameTitle: String = "")

    fun queueBoxFaceCache(urls: List<String>, rommId: Long, gameTitle: String = "", face: BoxFace)

    fun queueCoverCacheByGameId(url: String, gameId: Long)

    fun queueCoverCacheByGameId(urls: List<String>, gameId: Long)

    suspend fun cacheGameImagesNow(rommId: Long, gameTitle: String, coverUrls: List<String>, backgroundUrls: List<String>, boxBackUrls: List<String> = emptyList(), boxSpineUrls: List<String> = emptyList(), logoUrls: List<String> = emptyList(), box3dUrls: List<String> = emptyList()): CachedGameImages

    suspend fun applyArtOverride(gameId: Long, slot: ArtSlot, url: String): Boolean

    suspend fun applyArtOverrideFromFile(gameId: Long, slot: ArtSlot, path: String): Boolean

    suspend fun clearArtOverride(gameId: Long, slot: ArtSlot)

    fun queueBadgeCache(achievementId: Long, badgeUrl: String, badgeUrlLock: String?)

    fun resumePendingBadgeCache()

    suspend fun setScreenshotAsBackground(gameId: Long, screenshotPath: String): Boolean

    fun queueAppIconCache(gameId: Long, packageName: String)

    suspend fun cacheAppIconSync(packageName: String): String?

    suspend fun validateAndCleanCache(force: Boolean = false, onProgress: (suspend (phase: String, current: Int, total: Int) -> Unit)? = null): CacheValidationResult

    fun needsFlatToShardedMigration(): Boolean

    suspend fun migrateFlatToSharded(onProgress: (suspend (current: Int, total: Int) -> Unit)? = null)
}
