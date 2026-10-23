package com.nendo.argosy.data.platform

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.getLaunchIntentForPackage
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
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
import com.nendo.argosy.data.local.dao.GameDao
import com.nendo.argosy.data.local.dao.PlatformDao
import com.nendo.argosy.data.preferences.UserPreferencesRepository
import com.nendo.argosy.di.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlatformFolderResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val platformDao: PlatformDao,
    private val gameDao: GameDao,
    private val preferencesRepository: UserPreferencesRepository
) {
    val defaultStorageBase: File by lazy {
        defaultRomStorageBase(context)
    }

    suspend fun storageBase(): File =
        preferencesRepository.userPreferences.first().romStoragePath?.let { File(it) }
            ?: defaultStorageBase

    suspend fun downloadDirForPlatform(platformId: Long, fallbackSlug: String): File =
        platformDownloadDir(platformDao.getById(platformId), fallbackSlug, storageBase())

    suspend fun downloadDirForGame(gameId: Long, fallbackSlug: String): File {
        val platformId = gameDao.getById(gameId)?.platformId
            ?: return platformDownloadDir(null, fallbackSlug, storageBase())
        return downloadDirForPlatform(platformId, fallbackSlug)
    }

    suspend fun romRootsForPlatform(platformId: Long, fallbackSlug: String): List<File> {
        val base = storageBase()
        val platform = platformDao.getById(platformId) ?: return listOf(File(base, fallbackSlug))
        return platformRomRoots(platform, base, platformDao.getAllPlatforms())
    }
}
