package com.nendo.argosy.data.launcher

import com.nendo.argosy.platform.PlatformContext as Context
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
import com.nendo.argosy.domain.model.LaunchPromptOption

expect object SteamLaunchers {
    val all: List<SteamLauncher>

    fun getInstalled(context: Context): List<SteamLauncher>

    fun getByPackage(packageName: String): SteamLauncher?

    fun getPreferred(context: Context): SteamLauncher?

    fun promptOptionFor(packageName: String): LaunchPromptOption?

    fun forPromptOption(option: LaunchPromptOption): SteamLauncher?

    fun launchChoices(pinned: Boolean, reporting: List<String>, isInstalled: (String) -> Boolean): List<String>

    fun displayNameForPackage(launcherPackage: String?): String
}
