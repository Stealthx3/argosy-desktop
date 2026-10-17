package com.nendo.argosy.data.repository

import com.nendo.argosy.platform.PlatformIntentType as Intent
import com.nendo.argosy.platform.action
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.data
import com.nendo.argosy.platform.flags
import com.nendo.argosy.platform.putExtra
import com.nendo.argosy.platform.component
import com.nendo.argosy.platform.`package`
import com.nendo.argosy.platform.getStringExtra
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.PlatformIntent
import kotlinx.coroutines.flow.Flow

expect class AppsRepository {
    val packageChanges: Flow<String>

    suspend fun getInstalledApps(includeSystemApps: Boolean = false): List<InstalledApp>

    fun getLaunchIntent(packageName: String): Intent?

    fun getAppLabel(packageName: String): String?

    fun isArgosy(packageName: String): Boolean
}
