package com.nendo.argosy.platform

import com.nendo.argosy.data.apps.HostAppLaunch

actual class PlatformBundle

actual fun PlatformContext.startActivity(intent: PlatformIntentType) {
    if (HostAppLaunch.launch(intent) || HostAppLaunch.openStorePage(intent)) return
    val uri = intent.data?.toString() ?: return
    if (intent.action == PlatformIntent.ACTION_VIEW && (uri.startsWith("https://") || uri.startsWith("http://"))) {
        runCatching { java.awt.Desktop.getDesktop().browse(java.net.URI(uri)) }
    }
}

actual fun PlatformContext.startActivity(intent: PlatformIntentType, options: PlatformBundle?) = startActivity(intent)
