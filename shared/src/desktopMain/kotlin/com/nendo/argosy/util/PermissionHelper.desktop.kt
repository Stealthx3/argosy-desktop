package com.nendo.argosy.util

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.data.emulator.PresenceEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class PermissionHelper @Inject constructor() {
    actual data class SessionDurations(
        actual val foregroundMs: Long,
        actual val screenOnMs: Long
    ) {
        actual val hasData: Boolean get() = foregroundMs >= 0 && screenOnMs >= 0
    }

    actual companion object {}

    actual fun hasUsageStatsPermission(context: Context): Boolean = false

    actual fun openUsageStatsSettings(context: Context) {
    }

    actual fun isPackageInForeground(context: Context, packageName: String, withinMs: Long): Boolean = false

    actual fun lastForegroundTimestamp(context: Context, packageName: String): Long? = null

    actual fun lastForegroundTimestamps(context: Context, packageNames: Set<String>, lookbackMs: Long): Map<String, Long> = emptyMap()

    actual fun isPackageOnScreen(context: Context, packageName: String, lookbackMs: Long): Boolean? = null

    actual fun isPackageOnScreenOrRecent(context: Context, packageName: String, recencyMs: Long): Boolean = false

    actual fun presenceEvents(context: Context, fromMs: Long, toMs: Long): List<PresenceEvent> = emptyList()

    actual fun currentForegroundPackage(context: Context, lookbackMs: Long): String? = null

    actual fun getSessionDurations(context: Context, packageName: String, fromMs: Long, toMs: Long): SessionDurations = TODO("desktop: getSessionDurations")
}
