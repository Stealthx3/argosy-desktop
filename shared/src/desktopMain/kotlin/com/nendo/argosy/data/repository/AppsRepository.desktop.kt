package com.nendo.argosy.data.repository

import com.nendo.argosy.data.apps.HostAppLaunch
import com.nendo.argosy.data.apps.HostApplications
import com.nendo.argosy.data.model.SandboxPermission
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import com.nendo.argosy.platform.PlatformIntentType as Intent

@Singleton
actual class AppsRepository @Inject constructor() {
    actual val packageChanges: Flow<String> = flow {
        emit("")
        var last = HostApplications.fingerprint()
        while (true) {
            delay(CHANGE_POLL_MS)
            val current = HostApplications.fingerprint()
            if (current != null && current != last) {
                last = current
                emit("")
            }
        }
    }.flowOn(Dispatchers.IO)

    actual suspend fun getInstalledApps(includeSystemApps: Boolean): List<InstalledApp> = withContext(Dispatchers.IO) {
        HostApplications.all()
            .asSequence()
            .map { app ->
                InstalledApp(
                    packageName = app.id,
                    label = app.name,
                    isSystemApp = app.isSystem || isArgosy(app.id),
                    declaresGameCategory = app.isGame
                )
            }
            .filter { includeSystemApps || !it.isSystemApp }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    actual fun getLaunchIntent(packageName: String): Intent? =
        HostApplications.find(packageName)?.let(HostAppLaunch::intent)

    actual fun getAppLabel(packageName: String): String? = HostApplications.find(packageName)?.name

    actual fun isArgosy(packageName: String): Boolean =
        ARGOSY_DESKTOP_IDS.any { packageName == it || packageName.startsWith("$it.") }

    actual fun isPackageUpdating(packageName: String): Boolean = false

    actual fun isPackageMissing(packageName: String): Boolean = false
}

private val ARGOSY_DESKTOP_IDS = listOf(SandboxPermission.APP_ID, "io.github.stealthx3.IceBreaker", "io.github.stealthx3.ArgosyDesktop")
private const val CHANGE_POLL_MS = 10_000L
