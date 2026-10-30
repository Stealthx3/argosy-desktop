package com.nendo.argosy.platform

import com.nendo.argosy.data.apps.HostAppLaunch

actual class PlatformBundle

actual fun PlatformContext.startActivity(intent: PlatformIntentType) {
    if (HostAppLaunch.launch(intent) || HostAppLaunch.openStorePage(intent)) return
    val uri = intent.data?.toString() ?: return
    if (intent.action == PlatformIntent.ACTION_VIEW && (uri.startsWith("https://") || uri.startsWith("http://"))) {
        runCatching { HostShell.spawn(listOf("xdg-open", uri), null) }
    }
}

actual fun PlatformContext.startActivity(intent: PlatformIntentType, options: PlatformBundle?) = startActivity(intent)
