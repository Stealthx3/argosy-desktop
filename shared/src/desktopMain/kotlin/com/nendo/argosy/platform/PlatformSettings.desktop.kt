package com.nendo.argosy.platform

import java.io.File
import java.security.MessageDigest

actual abstract class PlatformContentResolver

private object DesktopContentResolver : PlatformContentResolver()

actual val PlatformContext.contentResolver: PlatformContentResolver get() = DesktopContentResolver

actual object PlatformSettings {
    actual object Secure {
        actual val ANDROID_ID: String = "android_id"

        private val deviceId: String by lazy {
            val machineId = listOf("/etc/machine-id", "/var/lib/dbus/machine-id")
                .map(::File)
                .firstOrNull { it.canRead() }
                ?.readText()
                ?.trim()
                .orEmpty()
                .ifEmpty { System.getProperty("user.name") + "@" + java.net.InetAddress.getLocalHost().hostName }
            MessageDigest.getInstance("SHA-256")
                .digest("argosy-desktop:$machineId".toByteArray())
                .take(ANDROID_ID_BYTES)
                .joinToString("") { "%02x".format(it) }
        }

        actual fun getString(resolver: PlatformContentResolver, name: String): String? =
            if (name == ANDROID_ID) deviceId else null

        private const val ANDROID_ID_BYTES = 8
    }
}
