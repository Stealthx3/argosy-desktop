package com.nendo.argosy.data.repository

import com.nendo.argosy.platform.PlatformIntentType as Intent
import com.nendo.argosy.platform.PlatformIntent
import kotlinx.coroutines.flow.Flow

expect class AppsRepository {
    val packageChanges: Flow<String>

    suspend fun getInstalledApps(includeSystemApps: Boolean = false): List<InstalledApp>

    fun getLaunchIntent(packageName: String): Intent?

    fun getAppLabel(packageName: String): String?

    fun isArgosy(packageName: String): Boolean
}
