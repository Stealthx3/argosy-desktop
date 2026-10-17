package com.nendo.argosy.data.wallpaper

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.platform.PlatformIntentType as Intent
import com.nendo.argosy.platform.PlatformIntent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class LockScreenScenes @Inject constructor() {
    actual companion object {
        actual fun isLiveActive(context: Context): Boolean = false

        actual fun liveSlots(context: Context): Int = 0

        actual fun canOffer(context: Context, lockScreenArt: Boolean): Boolean = false

        actual fun isSupported(context: Context): Boolean = false

        actual fun pickerIntent(context: Context): Intent = TODO("desktop: pickerIntent")
    }

    actual fun clear() {
    }
}
