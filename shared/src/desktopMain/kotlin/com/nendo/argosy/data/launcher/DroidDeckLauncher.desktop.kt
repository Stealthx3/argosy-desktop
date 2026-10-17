package com.nendo.argosy.data.launcher

import com.nendo.argosy.platform.PlatformIntentType as Intent



actual object DroidDeckLauncher : SteamLauncher {
    override fun createLaunchIntent(steamAppId: Long): Intent = Intent()

    override val packageName: String = "com.droiddeck.launcher"

    override val displayName: String = "DroidDeck"
}
