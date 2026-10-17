package com.nendo.argosy.data.launcher



actual object GameNativeLauncher : SteamLauncher {
    override val packageName: String = "app.gamenative"

    override val displayName: String = "GameNative"
}
