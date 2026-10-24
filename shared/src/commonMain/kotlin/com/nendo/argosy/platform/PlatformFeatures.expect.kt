package com.nendo.argosy.platform

/**
 * Features this platform offers. Where one is false its entry points are hidden: Steam, Jellyfin
 * media, Argosy Social with QuayPass check-in, the device settings shortcut, and built-in
 * emulation with its settings, core downloads and first-run cores step.
 * [desktopWindow] is true where Argosy runs in a resizable window of its own (window and full
 * screen settings); [darkThemeByDefault] chooses Dark over System until the user picks a theme.
 */
expect object PlatformFeatures {
    val steam: Boolean
    val jellyfin: Boolean
    val social: Boolean
    val deviceSettings: Boolean
    val builtInEmulation: Boolean
    val desktopWindow: Boolean
    val darkThemeByDefault: Boolean
}
