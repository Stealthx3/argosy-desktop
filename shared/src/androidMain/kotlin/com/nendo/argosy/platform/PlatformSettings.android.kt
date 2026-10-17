package com.nendo.argosy.platform

import android.content.ContentResolver
import android.provider.Settings

actual typealias PlatformContentResolver = ContentResolver

actual val PlatformContext.contentResolver: PlatformContentResolver get() = contentResolver

actual object PlatformSettings {
    actual val ACTION_APPLICATION_DETAILS_SETTINGS: String get() = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    actual val ACTION_APP_NOTIFICATION_SETTINGS: String get() = Settings.ACTION_APP_NOTIFICATION_SETTINGS
    actual val ACTION_MANAGE_OVERLAY_PERMISSION: String get() = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
    actual val ACTION_MANAGE_WRITE_SETTINGS: String get() = Settings.ACTION_MANAGE_WRITE_SETTINGS
    actual val ACTION_SETTINGS: String get() = Settings.ACTION_SETTINGS
    actual val ACTION_USAGE_ACCESS_SETTINGS: String get() = Settings.ACTION_USAGE_ACCESS_SETTINGS
    actual val EXTRA_APP_PACKAGE: String get() = Settings.EXTRA_APP_PACKAGE

    actual object Secure {
        actual val ANDROID_ID: String get() = Settings.Secure.ANDROID_ID

        actual fun getString(resolver: PlatformContentResolver, name: String): String? =
            Settings.Secure.getString(resolver, name)
    }

    actual object System {
        actual val CONTENT_URI: PlatformUri get() = Settings.System.CONTENT_URI
    }
}
