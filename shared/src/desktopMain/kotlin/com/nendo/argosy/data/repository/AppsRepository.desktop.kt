package com.nendo.argosy.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import com.nendo.argosy.platform.PlatformIntentType as Intent
import com.nendo.argosy.platform.PlatformIntent

@Singleton
actual class AppsRepository @Inject constructor() {
    actual val packageChanges: Flow<String> = kotlinx.coroutines.flow.emptyFlow()

    actual suspend fun getInstalledApps(includeSystemApps: Boolean): List<InstalledApp> = emptyList()

    actual fun getAppLabel(packageName: String): String? = null

    actual fun isArgosy(packageName: String): Boolean = false

    actual fun getLaunchIntent(packageName: String): Intent? = null
}
