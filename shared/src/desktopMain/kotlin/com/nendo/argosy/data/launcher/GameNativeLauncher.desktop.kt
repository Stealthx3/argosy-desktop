package com.nendo.argosy.data.launcher

import com.nendo.argosy.platform.PlatformIntentType as Intent
import com.nendo.argosy.platform.PlatformIntent

actual object GameNativeLauncher : SteamLauncher {
    actual fun createCustomGameLaunchIntent(appId: Int): Intent = Intent()

    actual fun createSourcedLaunchIntent(appId: Int, gameSource: String): Intent = Intent()

    override fun createLaunchIntent(steamAppId: Long): Intent = Intent()

    override val packageName: String = "app.gamenative"

    override val displayName: String = "GameNative"
}
