package com.nendo.argosy.platform

expect abstract class PlatformContentResolver

expect val PlatformContext.contentResolver: PlatformContentResolver

expect object PlatformSettings {
    val ACTION_APPLICATION_DETAILS_SETTINGS: String
    val ACTION_APP_NOTIFICATION_SETTINGS: String
    val ACTION_MANAGE_OVERLAY_PERMISSION: String
    val ACTION_MANAGE_WRITE_SETTINGS: String
    val ACTION_SETTINGS: String
    val ACTION_USAGE_ACCESS_SETTINGS: String
    val EXTRA_APP_PACKAGE: String

    object Secure {
        val ANDROID_ID: String

        fun getString(resolver: PlatformContentResolver, name: String): String?
    }

    object System {
        val CONTENT_URI: PlatformUri
    }
}
