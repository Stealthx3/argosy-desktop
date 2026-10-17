package com.nendo.argosy.util

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources

expect object HomeAppComponent {
    val ALIAS_CLASS: String

    fun setEnabled(context: Context, enabled: Boolean)

    fun isLauncherActivity(className: String?): Boolean

    fun componentEnabled(available: Boolean, isDefaultHome: Boolean): Boolean

    fun startupAvailability(stored: Boolean, defaultApplied: Boolean, desktopModeSupported: Boolean, isDefaultHome: Boolean): HomeAppStartup
}
