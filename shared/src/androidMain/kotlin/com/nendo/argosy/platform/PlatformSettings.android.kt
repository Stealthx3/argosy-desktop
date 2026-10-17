package com.nendo.argosy.platform

import android.content.ContentResolver
import android.provider.Settings

actual typealias PlatformContentResolver = ContentResolver

actual val PlatformContext.contentResolver: PlatformContentResolver get() = contentResolver

actual object PlatformSettings {
    actual object Secure {
        actual val ANDROID_ID: String get() = Settings.Secure.ANDROID_ID

        actual fun getString(resolver: PlatformContentResolver, name: String): String? =
            Settings.Secure.getString(resolver, name)
    }
}
