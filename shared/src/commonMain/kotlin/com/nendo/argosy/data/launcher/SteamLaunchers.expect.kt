package com.nendo.argosy.data.launcher

import com.nendo.argosy.platform.PlatformContext as Context
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
