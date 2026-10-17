package com.nendo.argosy.data.launcher

import com.nendo.argosy.platform.PlatformIntentType as Intent
import com.nendo.argosy.platform.PlatformIntent
import javax.inject.Inject

actual interface SteamLauncher {
    actual val packageName: String

    actual val displayName: String

    actual fun createLaunchIntent(steamAppId: Long): Intent
}
