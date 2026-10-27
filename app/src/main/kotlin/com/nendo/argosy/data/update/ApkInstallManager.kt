package com.nendo.argosy.data.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.nendo.argosy.data.cache.ImageCacheManager
import com.nendo.argosy.data.download.DownloadCompletionEvent
import com.nendo.argosy.data.download.DownloadManager
import com.nendo.argosy.data.local.dao.GameArtDao
import com.nendo.argosy.data.local.dao.GameDao
import com.nendo.argosy.data.local.dao.resolved
import com.nendo.argosy.data.model.GameSource
import com.nendo.argosy.data.platform.LocalPlatformIds
import com.nendo.argosy.data.repository.AppsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import com.nendo.argosy.util.SafeCoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ApkInstallManager"

sealed class ApkInstallState {
    data object Idle : ApkInstallState()
    data class WaitingForInstall(val gameId: Long, val apkPath: String) : ApkInstallState()
    data class InstallComplete(val gameId: Long, val packageName: String) : ApkInstallState()
    data class InstallFailed(val gameId: Long, val reason: String) : ApkInstallState()
}

@Singleton
class ApkInstallManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadManager: DownloadManager,
    private val gameDao: GameDao,
    private val gameArtDao: GameArtDao,
    private val appInstaller: AppInstaller,
    private val imageCacheManager: ImageCacheManager,
    private val appsRepository: AppsRepository
) {
    private val scope = SafeCoroutineScope(Dispatchers.Main, "ApkInstallManager")

    private val _state = MutableStateFlow<ApkInstallState>(ApkInstallState.Idle)
    val state: StateFlow<ApkInstallState> = _state.asStateFlow()

    init {
        observeDownloadCompletions()
        observePackageChanges()
    }

    private fun observePackageChanges() {
        scope.launch {
            appsRepository.packageChanges.collect { packageName ->
                try {
                    linkInstalledApks(packageName.ifEmpty { null })
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to link installed APKs: ${e.message}")
                }
            }
        }
    }

    private suspend fun linkInstalledApks(changedPackage: String?) {
        val awaiting = gameDao.getDownloadedApksAwaitingInstall(LocalPlatformIds.ANDROID)
            .filter { changedPackage == null || it.titleId == changedPackage }
        awaiting.forEach { game ->
            val packageName = game.titleId ?: return@forEach
            if (appsRepository.isPackageMissing(packageName)) return@forEach
            linkGameToPackage(game.id, packageName)
        }
    }

    private fun observeDownloadCompletions() {
        scope.launch {
            downloadManager.completionEvents.collect { event ->
                handleDownloadCompletion(event)
            }
        }
    }

    private suspend fun handleDownloadCompletion(event: DownloadCompletionEvent) {
        val game = gameDao.getById(event.gameId) ?: return

        if (game.platformId != LocalPlatformIds.ANDROID) return
        if (!event.localPath.endsWith(".apk", ignoreCase = true)) return

        Log.d(TAG, "APK download complete: ${event.localPath}")

        val apkFile = File(event.localPath)
        if (!apkFile.exists()) {
            Log.e(TAG, "APK file not found: ${event.localPath}")
            return
        }

        val expectedPackageName = getApkPackageName(apkFile)
        if (expectedPackageName == null) {
            Log.e(TAG, "Could not read package name from APK")
            return
        }

        if (game.titleId != expectedPackageName) {
            gameDao.update(game.copy(titleId = expectedPackageName))
        }

        _state.value = ApkInstallState.WaitingForInstall(event.gameId, event.localPath)
        appInstaller.installApk(context, apkFile)
    }

    private fun getApkPackageName(apkFile: File): String? {
        return try {
            val packageInfo: PackageInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            }
            packageInfo?.packageName
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get package name from APK: ${e.message}")
            null
        }
    }

    private suspend fun linkGameToPackage(gameId: Long, packageName: String) {
        val game = gameDao.getById(gameId) ?: return
        val apkPath = game.localPath
        val holder = gameDao.getByPackageName(packageName)?.takeIf { it.id != game.id }
        if (holder != null && holder.rommId != null) {
            Log.w(TAG, "Package $packageName already owned by game ${holder.id} with rommId, leaving ${game.id} unlinked")
            _state.value = ApkInstallState.InstallFailed(game.id, packageName)
            return
        }
        if (holder != null) {
            gameArtDao.fillMissingFrom(targetGameId = game.id, sourceGameId = holder.id)
            gameDao.delete(holder.id)
        }
        gameDao.update(
            game.copy(
                packageName = packageName,
                titleId = packageName,
                source = GameSource.ANDROID_APP,
                localPath = null,
                isFavorite = game.isFavorite || holder?.isFavorite == true,
                playCount = game.playCount + (holder?.playCount ?: 0),
                playTimeMinutes = game.playTimeMinutes + (holder?.playTimeMinutes ?: 0),
                lastPlayed = listOfNotNull(game.lastPlayed, holder?.lastPlayed).maxOrNull()
            )
        )

        if (gameArtDao.resolved(game.id).coverPath.isNullOrBlank()) {
            imageCacheManager.queueAppIconCache(game.id, packageName)
        }
        Log.d(TAG, "Game ${game.title} linked to package $packageName" +
            if (holder != null) " (merged duplicate ${holder.id})" else "")

        apkPath?.let { File(it) }?.takeIf { it.exists() }?.let { apk ->
            if (apk.delete()) Log.d(TAG, "Deleted APK file: ${apk.absolutePath}")
        }
        _state.value = ApkInstallState.InstallComplete(game.id, packageName)
    }

    fun canInstallPackages(): Boolean {
        return appInstaller.canInstallPackages(context)
    }

    fun openInstallPermissionSettings() {
        appInstaller.openInstallPermissionSettings(context)
    }

    fun resetState() {
        _state.value = ApkInstallState.Idle
    }

    suspend fun installApkForGame(gameId: Long): Boolean {
        val game = gameDao.getById(gameId) ?: return false
        val localPath = game.localPath ?: return false

        if (!localPath.endsWith(".apk", ignoreCase = true)) return false

        val apkFile = File(localPath)
        if (!apkFile.exists()) return false

        val expectedPackageName = getApkPackageName(apkFile) ?: return false

        if (game.titleId != expectedPackageName) {
            gameDao.update(game.copy(titleId = expectedPackageName))
        }

        _state.value = ApkInstallState.WaitingForInstall(gameId, localPath)
        appInstaller.installApk(context, apkFile)
        return true
    }
}
