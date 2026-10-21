package com.nendo.argosy.platform

/**
 * Sets the app's per-app language through the framework, as LocaleManager.applicationLocales does
 * on Android 13+. A null tag clears it, following the system language again.
 */
expect fun setApplicationLocales(context: PlatformContext, tag: String?)
