package com.nendo.argosy.platform

/**
 * Features this platform offers; where one is false its entry points are hidden. [desktopWindow]
 * marks a resizable window of Argosy's own, [darkThemeByDefault] picks Dark until the user chooses.
 */
expect object PlatformFeatures {
    val steam: Boolean
    val jellyfin: Boolean
    val social: Boolean
    val deviceSettings: Boolean
    val builtInEmulation: Boolean
    val dualScreen: Boolean
    val selfUpdate: Boolean
    val systemApp: Boolean
    val desktopWindow: Boolean
    val darkThemeByDefault: Boolean
}
