package com.nendo.argosy.platform

import java.io.File
import java.net.NetworkInterface

actual fun PlatformContext.getSystemService(name: String): Any? = when (name) {
    CONNECTIVITY_SERVICE -> DesktopConnectivityManager
    USER_SERVICE -> DesktopUserManager
    else -> null
}

actual fun PlatformContext.getExternalFilesDir(type: String?): File? =
    filesDir.resolveSibling("external").let { if (type == null) it else it.resolve(type) }.also { it.mkdirs() }

actual val PlatformContext.packageManager: PlatformPackageManager get() = DesktopPackageManager

actual abstract class PlatformPackageManager

private object DesktopPackageManager : PlatformPackageManager()

actual fun PlatformPackageManager.getPackageInfo(packageName: String, flags: Int): PlatformPackageInfo =
    throw IllegalStateException("$packageName is not an installed package on the desktop")

actual fun PlatformPackageManager.getApplicationInfo(packageName: String, flags: Int): PlatformApplicationInfo =
    throw IllegalStateException("$packageName is not an installed package on the desktop")

actual fun PlatformPackageManager.getApplicationLabel(info: PlatformApplicationInfo): CharSequence = ""

actual open class PlatformApplicationInfo

actual open class PlatformPackageInfo

actual val PlatformPackageInfo.versionName: String? get() = null

actual open class PlatformUserManager

private object DesktopUserManager : PlatformUserManager()

actual val PlatformUserManager.isUserUnlocked: Boolean get() = true

actual open class PlatformConnectivityManager

private object DesktopConnectivityManager : PlatformConnectivityManager()

actual open class PlatformNetwork

private object DesktopNetwork : PlatformNetwork()

actual class PlatformNetworkCapabilities

private val internetCapabilities = PlatformNetworkCapabilities()

actual open class PlatformNetworkCallback actual constructor() {
    actual open fun onAvailable(network: PlatformNetwork) {
    }

    actual open fun onLost(network: PlatformNetwork) {
    }
}

private fun hasUsableInterface(): Boolean = runCatching {
    NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().any { it.isUp && !it.isLoopback }
}.getOrDefault(false)

actual val PlatformConnectivityManager.activeNetwork: PlatformNetwork?
    get() = if (hasUsableInterface()) DesktopNetwork else null

actual fun PlatformConnectivityManager.getNetworkCapabilities(network: PlatformNetwork?): PlatformNetworkCapabilities? =
    if (network != null) internetCapabilities else null

actual fun PlatformConnectivityManager.registerDefaultNetworkCallback(networkCallback: PlatformNetworkCallback) {
    if (hasUsableInterface()) {
        Thread({ networkCallback.onAvailable(DesktopNetwork) }, "desktop-network-callback").apply { isDaemon = true }.start()
    }
}

actual fun PlatformConnectivityManager.unregisterNetworkCallback(networkCallback: PlatformNetworkCallback) {
}

actual fun PlatformNetworkCapabilities.hasCapability(capability: Int): Boolean = capability == NET_CAPABILITY_INTERNET
