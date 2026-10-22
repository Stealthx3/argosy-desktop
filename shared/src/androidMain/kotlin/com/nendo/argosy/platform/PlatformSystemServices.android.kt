package com.nendo.argosy.platform

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.UserManager
import java.io.File

actual fun PlatformContext.getSystemService(name: String): Any? = getSystemService(name)

actual fun PlatformContext.getExternalFilesDir(type: String?): File? = getExternalFilesDir(type)

actual val PlatformContext.externalCacheDir: File? get() = externalCacheDir

actual val PlatformContext.dataDir: File get() = dataDir

actual val PlatformContext.packageManager: PlatformPackageManager get() = packageManager

actual typealias PlatformPackageManager = PackageManager

actual fun PlatformPackageManager.getPackageInfo(packageName: String, flags: Int): PlatformPackageInfo =
    getPackageInfo(packageName, flags)

actual fun PlatformPackageManager.getApplicationInfo(packageName: String, flags: Int): PlatformApplicationInfo =
    getApplicationInfo(packageName, flags)

actual fun PlatformPackageManager.getApplicationLabel(info: PlatformApplicationInfo): CharSequence = getApplicationLabel(info)

actual fun PlatformPackageManager.getLaunchIntentForPackage(packageName: String): PlatformIntentType? =
    getLaunchIntentForPackage(packageName)

actual typealias PlatformApplicationInfo = android.content.pm.ApplicationInfo

actual typealias PlatformPackageInfo = PackageInfo

actual val PlatformPackageInfo.versionName: String? get() = versionName

actual typealias PlatformUserManager = UserManager

actual val PlatformUserManager.isUserUnlocked: Boolean get() = isUserUnlocked

actual typealias PlatformConnectivityManager = ConnectivityManager

actual typealias PlatformNetwork = Network

actual typealias PlatformNetworkCapabilities = NetworkCapabilities

actual typealias PlatformNetworkCallback = ConnectivityManager.NetworkCallback

actual val PlatformConnectivityManager.activeNetwork: PlatformNetwork? get() = activeNetwork

actual fun PlatformConnectivityManager.getNetworkCapabilities(network: PlatformNetwork?): PlatformNetworkCapabilities? =
    getNetworkCapabilities(network)

actual fun PlatformConnectivityManager.registerDefaultNetworkCallback(networkCallback: PlatformNetworkCallback) =
    registerDefaultNetworkCallback(networkCallback)

actual fun PlatformConnectivityManager.unregisterNetworkCallback(networkCallback: PlatformNetworkCallback) =
    unregisterNetworkCallback(networkCallback)

actual fun PlatformNetworkCapabilities.hasCapability(capability: Int): Boolean = hasCapability(capability)
