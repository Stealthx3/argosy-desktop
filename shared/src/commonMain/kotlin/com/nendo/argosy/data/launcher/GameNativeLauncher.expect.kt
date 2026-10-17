package com.nendo.argosy.data.launcher

import com.nendo.argosy.platform.PlatformIntentType as Intent
import com.nendo.argosy.platform.PlatformIntent

expect object GameNativeLauncher : SteamLauncher {
    fun createCustomGameLaunchIntent(appId: Int): Intent

    fun createSourcedLaunchIntent(appId: Int, gameSource: String): Intent
}
