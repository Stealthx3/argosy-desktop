package com.nendo.argosy.libretro

import com.nendo.argosy.data.local.entity.CoreVersionHistoryEntity
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class LibretroCoreManager @Inject constructor() {
    actual suspend fun migrateAbiIfNeeded() {
    }

    actual fun getCorePathForPlatform(platformSlug: String): String? = null

    actual fun getCorePathForPlatform(platformSlug: String, selectedCoreId: String?): String? = null

    actual fun getCorePathForCoreId(coreId: String): String? = null

    actual fun isCoreAvailable(platformSlug: String): Boolean = false

    actual fun isCoreInstalled(coreId: String): Boolean = false

    actual fun isPlatformSupported(platformSlug: String): Boolean = false

    actual fun getInstalledCores(): List<LibretroCoreRegistry.CoreInfo> = emptyList()

    actual fun getMissingCoresForPlatforms(platformSlugs: Set<String>): List<LibretroCoreRegistry.CoreInfo> = emptyList()

    actual suspend fun downloadCoreForPlatform(platformSlug: String): Result<String> = Result.failure(UnsupportedOperationException("not available on desktop"))

    actual suspend fun downloadCoreForPlatform(platformSlug: String, selectedCoreId: String?, onProgress: ((Float) -> Unit)?): Result<String> = Result.failure(UnsupportedOperationException("not available on desktop"))

    actual suspend fun downloadCoreById(coreId: String): Result<File> = Result.failure(UnsupportedOperationException("not available on desktop"))

    actual suspend fun downloadCore(coreInfo: LibretroCoreRegistry.CoreInfo, onProgress: ((Float) -> Unit)?): Result<File> = Result.failure(UnsupportedOperationException("not available on desktop"))

    actual suspend fun resolveNetplayCorePath(coreId: String, hostCoreHash: String): NetplayCoreResolution = TODO("desktop: resolveNetplayCorePath")

    actual suspend fun getCoreHistory(coreId: String): List<CoreVersionHistoryEntity> = emptyList()

    actual suspend fun rollbackToHistory(coreId: String, history: CoreVersionHistoryEntity): Boolean = false

    actual suspend fun blockInstalledVersion(coreId: String) {
    }

    actual suspend fun verifyDownloadedCore(coreInfo: LibretroCoreRegistry.CoreInfo): Boolean? = null

    actual fun getDownloadedCores(): List<LibretroCoreRegistry.CoreInfo> = emptyList()

    actual suspend fun deleteCore(coreId: String): Boolean = false

    actual fun getCoresDir(): File = TODO("desktop: getCoresDir")

    actual suspend fun getPendingCoreUpdates(): List<PendingCoreUpdate> = emptyList()

    actual suspend fun updateInstalledCores(onCoreStarted: (core: LibretroCoreRegistry.CoreInfo, position: Int, total: Int) -> Unit): CoreBulkUpdateResult = CoreBulkUpdateResult(updated = emptyList(), failed = emptyList())
}
