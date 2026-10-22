package com.nendo.argosy.data.launcher

import com.nendo.argosy.platform.PlatformContext as Context

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
actual fun SteamLauncher.isInstalled(context: Context): Boolean = this.isInstalled(context)
