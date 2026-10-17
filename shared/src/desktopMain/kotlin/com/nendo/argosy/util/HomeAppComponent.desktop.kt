package com.nendo.argosy.util

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources

actual object HomeAppComponent {
    actual const val ALIAS_CLASS: String = "com.nendo.argosy.MainActivity"

    actual fun setEnabled(context: Context, enabled: Boolean) {
    }

    actual fun isLauncherActivity(className: String?): Boolean = false

    actual fun componentEnabled(available: Boolean, isDefaultHome: Boolean): Boolean = false

    actual fun startupAvailability(stored: Boolean, defaultApplied: Boolean, desktopModeSupported: Boolean, isDefaultHome: Boolean): HomeAppStartup = HomeAppStartup(available = false, persistDefault = false)
}
