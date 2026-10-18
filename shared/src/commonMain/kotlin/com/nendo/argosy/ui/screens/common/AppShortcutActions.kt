package com.nendo.argosy.ui.screens.common

import com.nendo.argosy.platform.PlatformIntentType as Intent
import com.nendo.argosy.platform.className
import com.nendo.argosy.platform.component
import com.nendo.argosy.platform.`package`
import com.nendo.argosy.platform.getStringExtra
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.action
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.data
import com.nendo.argosy.platform.flags
import com.nendo.argosy.platform.putExtra
import com.nendo.argosy.platform.PlatformIntent
import com.nendo.argosy.platform.PlatformUri as Uri
import com.nendo.argosy.platform.authority
import com.nendo.argosy.platform.encodedPath
import com.nendo.argosy.platform.getQueryParameter
import com.nendo.argosy.platform.host
import com.nendo.argosy.platform.lastPathSegment
import com.nendo.argosy.platform.path
import com.nendo.argosy.platform.pathSegments
import com.nendo.argosy.platform.queryParameterNames
import com.nendo.argosy.platform.scheme
import com.nendo.argosy.platform.PlatformUris
import com.nendo.argosy.data.preferences.UserPreferencesRepository
import com.nendo.argosy.data.repository.AppsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppShortcutActions @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val appsRepository: AppsRepository
) {
    suspend fun isPinned(packageName: String): Boolean =
        packageName in preferencesRepository.preferences.first().secondaryHomeApps

    suspend fun togglePinned(packageName: String) {
        preferencesRepository.toggleDockPin(packageName)
    }

    suspend fun isHidden(packageName: String): Boolean {
        val prefs = preferencesRepository.preferences.first()
        return if (isSystemApp(packageName)) {
            packageName !in prefs.visibleSystemApps
        } else {
            packageName in prefs.hiddenApps
        }
    }

    suspend fun toggleHidden(packageName: String) {
        val prefs = preferencesRepository.preferences.first()
        if (isSystemApp(packageName)) {
            val visible = prefs.visibleSystemApps
            preferencesRepository.setVisibleSystemApps(
                if (packageName in visible) visible - packageName else visible + packageName
            )
        } else {
            val hidden = prefs.hiddenApps
            preferencesRepository.setHiddenApps(
                if (packageName in hidden) hidden - packageName else hidden + packageName
            )
        }
    }

    fun uninstallIntent(packageName: String): Intent =
        Intent(PlatformIntent.ACTION_DELETE, PlatformUris.parse("package:$packageName"))
            .addFlags(PlatformIntent.FLAG_ACTIVITY_NEW_TASK)

    private suspend fun isSystemApp(packageName: String): Boolean =
        appsRepository.getInstalledApps(includeSystemApps = true)
            .firstOrNull { it.packageName == packageName }
            ?.isSystemApp == true
}
