package com.nendo.argosy.platform

expect abstract class PlatformContentResolver

expect val PlatformContext.contentResolver: PlatformContentResolver

expect object PlatformSettings {
    object Secure {
        val ANDROID_ID: String

        fun getString(resolver: PlatformContentResolver, name: String): String?
    }
}
