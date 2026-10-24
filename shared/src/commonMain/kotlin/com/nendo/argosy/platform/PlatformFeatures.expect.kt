package com.nendo.argosy.platform

/**
 * Features this platform offers. Where one is false its entry points are hidden: Steam, Jellyfin
 * media, Argosy Social with QuayPass check-in, the device settings shortcut, and built-in
 * emulation with its settings, core downloads and first-run cores step.
 */
expect object PlatformFeatures {
    val steam: Boolean
    val jellyfin: Boolean
    val social: Boolean
    val deviceSettings: Boolean
    val builtInEmulation: Boolean
}
