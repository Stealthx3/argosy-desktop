package com.nendo.argosy.data.launcher

import javax.inject.Inject

actual interface SteamLauncher {
    actual val packageName: String

    actual val displayName: String
}
