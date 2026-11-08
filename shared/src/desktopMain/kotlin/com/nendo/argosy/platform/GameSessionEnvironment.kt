package com.nendo.argosy.platform

/**
 * The `flatpak-spawn --host` options that start a game in the display and Steam session Argosy
 * itself runs in under Steam Game Mode: the [FORWARDED] variables Argosy has, plus a cleared
 * `WAYLAND_DISPLAY` where Argosy has none. Empty outside Game Mode.
 */
object GameSessionEnvironment {
    private const val WAYLAND_DISPLAY = "WAYLAND_DISPLAY"

    val FORWARDED = listOf(
        "DISPLAY",
        WAYLAND_DISPLAY,
        "GAMESCOPE_WAYLAND_DISPLAY",
        "SteamGameId",
        "SteamAppId",
        "SteamOverlayGameId",
        "STEAM_GAME_DISPLAY_0",
        "ENABLE_GAMESCOPE_WSI"
    )

    fun spawnOptions(environment: Map<String, String>): List<String> {
        if (!DesktopWindowMode.isGamescope(environment)) return emptyList()
        val forwarded = FORWARDED.mapNotNull { name -> environment[name]?.let { "--env=$name=$it" } }
        val cleared = if (environment[WAYLAND_DISPLAY].isNullOrEmpty()) listOf("--unset-env=$WAYLAND_DISPLAY") else emptyList()
        return forwarded + cleared
    }
}
