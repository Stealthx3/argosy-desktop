package com.nendo.argosy.data.launcher

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.domain.model.LaunchPromptOption

actual object SteamLaunchers {
    actual val all: List<SteamLauncher> = emptyList()

    actual fun getInstalled(context: Context): List<SteamLauncher> = emptyList()

    actual fun getByPackage(packageName: String): SteamLauncher? = null

    actual fun getPreferred(context: Context): SteamLauncher? = null

    actual fun promptOptionFor(packageName: String): LaunchPromptOption? = null

    actual fun forPromptOption(option: LaunchPromptOption): SteamLauncher? = null

    actual fun launchChoices(pinned: Boolean, reporting: List<String>, isInstalled: (String) -> Boolean): List<String> = emptyList()

    actual fun displayNameForPackage(launcherPackage: String?): String = ""
}
