package com.nendo.argosy.data.launcher

import com.nendo.argosy.platform.PlatformIntentType as Intent
import com.nendo.argosy.platform.PlatformIntent

expect interface SteamLauncher {
    val packageName: String

    val displayName: String

    fun createLaunchIntent(steamAppId: Long): Intent
}
