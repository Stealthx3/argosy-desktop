package com.nendo.argosy.util

import com.nendo.argosy.platform.PlatformContext as Context
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
import com.nendo.argosy.data.emulator.PresenceEvent

expect class PermissionHelper {
    class SessionDurations {
        val foregroundMs: Long
        val screenOnMs: Long
        val hasData: Boolean
    }

    companion object {}

    fun hasUsageStatsPermission(context: Context): Boolean

    fun openUsageStatsSettings(context: Context)

    fun isPackageInForeground(context: Context, packageName: String, withinMs: Long = 5000): Boolean

    fun lastForegroundTimestamp(context: Context, packageName: String): Long?

    fun lastForegroundTimestamps(context: Context, packageNames: Set<String>, lookbackMs: Long = 24 * 60 * 60 * 1000L): Map<String, Long>

    fun isPackageOnScreen(context: Context, packageName: String, lookbackMs: Long = 10 * 60 * 1000L): Boolean?

    fun isPackageOnScreenOrRecent(context: Context, packageName: String, recencyMs: Long = 15_000): Boolean

    fun presenceEvents(context: Context, fromMs: Long, toMs: Long): List<PresenceEvent>

    fun currentForegroundPackage(context: Context, lookbackMs: Long = 5 * 60 * 1000L): String?

    fun getSessionDurations(context: Context, packageName: String, fromMs: Long, toMs: Long): SessionDurations
}
