package com.nendo.argosy.data.update

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import java.io.File

expect class AppInstaller {
    fun canInstallPackages(context: Context): Boolean

    fun openInstallPermissionSettings(context: Context)

    fun installApk(context: Context, apkFile: File)

    fun getApkCacheFile(context: Context, version: String): File

    fun clearCachedApks(context: Context)
}
