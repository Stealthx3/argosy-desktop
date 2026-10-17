package com.nendo.argosy.platform

import android.content.SharedPreferences

actual typealias PlatformSharedPreferences = SharedPreferences

actual fun PlatformContext.getSharedPreferences(name: String?, mode: Int): PlatformSharedPreferences =
    getSharedPreferences(name, mode)
