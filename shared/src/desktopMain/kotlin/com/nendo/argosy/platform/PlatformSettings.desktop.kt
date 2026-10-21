package com.nendo.argosy.platform

import java.io.File
import java.security.MessageDigest

actual abstract class PlatformContentResolver

private object DesktopContentResolver : PlatformContentResolver()

actual val PlatformContext.contentResolver: PlatformContentResolver get() = DesktopContentResolver

actual object PlatformSettings {
    actual val ACTION_APPLICATION_DETAILS_SETTINGS: String = "android.settings.APPLICATION_DETAILS_SETTINGS"
    actual val ACTION_APP_NOTIFICATION_SETTINGS: String = "android.settings.APP_NOTIFICATION_SETTINGS"
    actual val ACTION_MANAGE_OVERLAY_PERMISSION: String = "android.settings.action.MANAGE_OVERLAY_PERMISSION"
    actual val ACTION_MANAGE_WRITE_SETTINGS: String = "android.settings.action.MANAGE_WRITE_SETTINGS"
    actual val ACTION_SETTINGS: String = "android.settings.SETTINGS"
    actual val ACTION_USAGE_ACCESS_SETTINGS: String = "android.settings.USAGE_ACCESS_SETTINGS"
    actual val EXTRA_APP_PACKAGE: String = "android.provider.extra.APP_PACKAGE"

    actual fun canDrawOverlays(context: PlatformContext): Boolean = true

    actual object Secure {
        actual val ANDROID_ID: String = "android_id"

        private val deviceId: String by lazy {
            val machineId = listOf("/etc/machine-id", "/var/lib/dbus/machine-id")
                .map(::File)
                .firstOrNull { it.canRead() }
                ?.readText()
                ?.trim()
                .orEmpty()
                .ifEmpty { java.lang.System.getProperty("user.name") + "@" + java.net.InetAddress.getLocalHost().hostName }
            MessageDigest.getInstance("SHA-256")
                .digest("argosy-desktop:$machineId".toByteArray())
                .take(ANDROID_ID_BYTES)
                .joinToString("") { "%02x".format(it) }
        }

        actual fun getString(resolver: PlatformContentResolver, name: String): String? =
            if (name == ANDROID_ID) deviceId else null

        private const val ANDROID_ID_BYTES = 8
    }

    actual object System {
        actual val CONTENT_URI: PlatformUri = PlatformUris.parse("content://settings/system")

        actual fun canWrite(context: PlatformContext): Boolean = true
    }
}
