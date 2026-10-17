package com.nendo.argosy.platform

import java.io.File

const val CONNECTIVITY_SERVICE: String = "connectivity"

const val USER_SERVICE: String = "user"

const val NET_CAPABILITY_INTERNET: Int = 12

expect fun PlatformContext.getSystemService(name: String): Any?

expect fun PlatformContext.getExternalFilesDir(type: String?): File?

expect val PlatformContext.packageManager: PlatformPackageManager

expect abstract class PlatformPackageManager

expect fun PlatformPackageManager.getPackageInfo(packageName: String, flags: Int): PlatformPackageInfo

expect fun PlatformPackageManager.getApplicationInfo(packageName: String, flags: Int): PlatformApplicationInfo

expect fun PlatformPackageManager.getApplicationLabel(info: PlatformApplicationInfo): CharSequence

expect open class PlatformApplicationInfo

expect open class PlatformPackageInfo

expect val PlatformPackageInfo.versionName: String?

expect open class PlatformUserManager

expect val PlatformUserManager.isUserUnlocked: Boolean

expect open class PlatformConnectivityManager

expect open class PlatformNetwork

expect class PlatformNetworkCapabilities

expect open class PlatformNetworkCallback() {
    open fun onAvailable(network: PlatformNetwork)
    open fun onLost(network: PlatformNetwork)
}

expect val PlatformConnectivityManager.activeNetwork: PlatformNetwork?

expect fun PlatformConnectivityManager.getNetworkCapabilities(network: PlatformNetwork?): PlatformNetworkCapabilities?

expect fun PlatformConnectivityManager.registerDefaultNetworkCallback(networkCallback: PlatformNetworkCallback)

expect fun PlatformConnectivityManager.unregisterNetworkCallback(networkCallback: PlatformNetworkCallback)

expect fun PlatformNetworkCapabilities.hasCapability(capability: Int): Boolean
