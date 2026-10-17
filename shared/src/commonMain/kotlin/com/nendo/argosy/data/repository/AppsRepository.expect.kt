package com.nendo.argosy.data.repository

import kotlinx.coroutines.flow.Flow

expect class AppsRepository {
    val packageChanges: Flow<String>

    suspend fun getInstalledApps(includeSystemApps: Boolean = false): List<InstalledApp>

    fun getAppLabel(packageName: String): String?

    fun isArgosy(packageName: String): Boolean
}
