package com.nendo.argosy.platform

/**
 * The `flatpak-spawn --host` options that start a game in the display and Steam session Argosy
 * itself runs in. Under Steam Game Mode: the [FORWARDED] variables Argosy has, plus a cleared
 * `WAYLAND_DISPLAY` where Argosy has none. Whenever Steam launched Argosy (`SteamGameId` set):
 * Steam's controller variables ([STEAM_INPUT], any `SDL_GAMECONTROLLER*`), which point a game's
 * SDL at Steam's virtual gamepad.
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

    val STEAM_INPUT = listOf(
        "SteamVirtualGamepadInfo",
        "EnableConfiguratorSupport",
        "SteamDeck"
    )

    private const val SDL_CONTROLLER_PREFIX = "SDL_GAMECONTROLLER"

    fun spawnOptions(environment: Map<String, String>): List<String> {
        val display = if (DesktopWindowMode.isGamescope(environment)) {
            val forwarded = FORWARDED.mapNotNull { name -> environment[name]?.let { "--env=$name=$it" } }
            val cleared = if (environment[WAYLAND_DISPLAY].isNullOrEmpty()) listOf("--unset-env=$WAYLAND_DISPLAY") else emptyList()
            forwarded + cleared
        } else {
            emptyList()
        }
        return display + steamInputOptions(environment, display)
    }

    private fun steamInputOptions(environment: Map<String, String>, already: List<String>): List<String> {
        if (environment["SteamGameId"].isNullOrEmpty()) return emptyList()
        val names = STEAM_INPUT.filter { it in environment } +
            environment.keys.filter { it.startsWith(SDL_CONTROLLER_PREFIX) }.sorted()
        return names.map { "--env=$it=${environment.getValue(it)}" }.filterNot { it in already }
    }
}
