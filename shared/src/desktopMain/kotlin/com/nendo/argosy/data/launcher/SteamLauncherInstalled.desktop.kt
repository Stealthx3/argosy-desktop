package com.nendo.argosy.data.launcher

import com.nendo.argosy.platform.PlatformContext as Context

actual fun SteamLauncher.isInstalled(context: Context): Boolean = false
