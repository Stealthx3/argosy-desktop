package com.nendo.argosy.platform

/**
 * Whether the desktop window opens full screen: always under gamescope, where F11 does nothing,
 * and elsewhere `--fullscreen` or `--windowed` when given, else the "Start in Full Screen" setting.
 */
object DesktopWindowMode {
    fun isGamescope(env: Map<String, String>): Boolean =
        env["XDG_CURRENT_DESKTOP"].equals(GAMESCOPE, ignoreCase = true) ||
            !env["GAMESCOPE_WAYLAND_DISPLAY"].isNullOrBlank()

    fun startsFullScreen(setting: Boolean, override: Boolean?, gamescope: Boolean): Boolean =
        gamescope || (override ?: setting)

    fun overrideFrom(args: Array<String>): Boolean? = when {
        FULL_SCREEN_FLAG in args -> true
        WINDOWED_FLAG in args -> false
        else -> null
    }

    private const val GAMESCOPE = "gamescope"
    private const val FULL_SCREEN_FLAG = "--fullscreen"
    private const val WINDOWED_FLAG = "--windowed"
}
