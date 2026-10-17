package com.nendo.argosy.libretro

import com.nendo.argosy.data.local.entity.CoreVersionHistoryEntity
import java.io.File

expect class LibretroCoreManager {
    suspend fun migrateAbiIfNeeded()

    fun getCorePathForPlatform(platformSlug: String): String?

    fun getCorePathForPlatform(platformSlug: String, selectedCoreId: String?): String?

    fun getCorePathForCoreId(coreId: String): String?

    fun isCoreAvailable(platformSlug: String): Boolean

    fun isCoreInstalled(coreId: String): Boolean

    fun isPlatformSupported(platformSlug: String): Boolean

    fun getInstalledCores(): List<LibretroCoreRegistry.CoreInfo>

    fun getMissingCoresForPlatforms(platformSlugs: Set<String>): List<LibretroCoreRegistry.CoreInfo>

    suspend fun downloadCoreForPlatform(platformSlug: String): Result<String>

    suspend fun downloadCoreForPlatform(platformSlug: String, selectedCoreId: String?, onProgress: ((Float) -> Unit)? = null): Result<String>

    suspend fun downloadCoreById(coreId: String): Result<File>

    suspend fun downloadCore(coreInfo: LibretroCoreRegistry.CoreInfo, onProgress: ((Float) -> Unit)? = null): Result<File>

    suspend fun resolveNetplayCorePath(coreId: String, hostCoreHash: String): NetplayCoreResolution

    suspend fun getCoreHistory(coreId: String): List<CoreVersionHistoryEntity>

    suspend fun rollbackToHistory(coreId: String, history: CoreVersionHistoryEntity): Boolean

    suspend fun blockInstalledVersion(coreId: String)

    suspend fun verifyDownloadedCore(coreInfo: LibretroCoreRegistry.CoreInfo): Boolean?

    fun getDownloadedCores(): List<LibretroCoreRegistry.CoreInfo>

    suspend fun deleteCore(coreId: String): Boolean

    fun getCoresDir(): File

    suspend fun getPendingCoreUpdates(): List<PendingCoreUpdate>

    suspend fun updateInstalledCores(onCoreStarted: (core: LibretroCoreRegistry.CoreInfo, position: Int, total: Int) -> Unit): CoreBulkUpdateResult
}
