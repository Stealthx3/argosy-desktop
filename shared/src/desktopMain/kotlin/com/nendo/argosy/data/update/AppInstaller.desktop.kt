package com.nendo.argosy.data.update

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class AppInstaller @Inject constructor() {
    actual fun canInstallPackages(context: Context): Boolean = false

    actual fun openInstallPermissionSettings(context: Context) {
    }

    actual fun installApk(context: Context, apkFile: File) {
    }

    actual fun getApkCacheFile(context: Context, version: String): File = TODO("desktop: getApkCacheFile")

    actual fun clearCachedApks(context: Context) {
    }
}
