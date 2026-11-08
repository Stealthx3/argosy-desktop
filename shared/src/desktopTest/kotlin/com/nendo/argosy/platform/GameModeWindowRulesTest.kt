package com.nendo.argosy.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameModeWindowRulesTest {
    @Test
    fun appIdIsSteamAppIdUnderGamescopeOnly() {
        val gameMode = mapOf("GAMESCOPE_WAYLAND_DISPLAY" to "gamescope-0", "SteamAppId" to "4231199437")
        assertEquals(4231199437L.toInt(), GameModeWindowRules.appId(gameMode))
        assertNull(GameModeWindowRules.appId(mapOf("SteamAppId" to "4231199437")))
        assertNull(GameModeWindowRules.appId(mapOf("GAMESCOPE_WAYLAND_DISPLAY" to "gamescope-0")))
        assertNull(GameModeWindowRules.appId(mapOf("GAMESCOPE_WAYLAND_DISPLAY" to "gamescope-0", "SteamAppId" to "0")))
        assertNull(GameModeWindowRules.appId(mapOf("GAMESCOPE_WAYLAND_DISPLAY" to "gamescope-0", "SteamAppId" to "x")))
    }
}
