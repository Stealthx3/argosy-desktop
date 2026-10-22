package com.nendo.argosy.util

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.getLaunchIntentForPackage
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
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
import com.nendo.argosy.data.preferences.DisplayPreferencesRepository
import com.nendo.argosy.util.Logger
import com.nendo.argosy.di.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeAppAvailability @Inject constructor(
    @ApplicationContext private val context: Context,
    private val displayPreferences: DisplayPreferencesRepository
) {

    suspend fun applyAtStartup() = withContext(Dispatchers.IO) {
        val stored = displayPreferences.preferences.first().homeAppAvailable
        val defaultApplied = displayPreferences.isHomeAppDefaultApplied()
        val isDefaultHome = SecondaryHomeComponent.isDefaultHome(context)
        val desktopModeSupported = DesktopMode.isSupported(context)
        val startup = HomeAppComponent.startupAvailability(
            stored = stored,
            defaultApplied = defaultApplied,
            desktopModeSupported = desktopModeSupported,
            isDefaultHome = isDefaultHome
        )
        Logger.info(
            TAG,
            "applyAtStartup | stored=$stored defaultApplied=$defaultApplied desktopModeSupported=$desktopModeSupported " +
                "isDefaultHome=$isDefaultHome -> available=${startup.available} persistDefault=${startup.persistDefault}"
        )
        if (startup.persistDefault) displayPreferences.setHomeAppAvailable(startup.available)
        HomeAppComponent.setEnabled(
            context,
            HomeAppComponent.componentEnabled(startup.available, isDefaultHome)
        )
    }

    private companion object {
        const val TAG = "HomeAppAvailability"
    }

    suspend fun isDefaultHome(): Boolean = withContext(Dispatchers.IO) {
        SecondaryHomeComponent.isDefaultHome(context)
    }

    suspend fun setAvailable(available: Boolean): Boolean = withContext(Dispatchers.IO) {
        val isDefaultHome = SecondaryHomeComponent.isDefaultHome(context)
        val applied = HomeAppComponent.componentEnabled(available, isDefaultHome)
        displayPreferences.setHomeAppAvailable(applied)
        HomeAppComponent.setEnabled(context, applied)
        applied
    }
}
