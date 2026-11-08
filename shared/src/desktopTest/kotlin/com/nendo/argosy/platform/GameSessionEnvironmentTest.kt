package com.nendo.argosy.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class GameSessionEnvironmentTest {
    @Test
    fun outsideGameModeNothingIsForwarded() {
        assertEquals(emptyList(), GameSessionEnvironment.spawnOptions(mapOf("DISPLAY" to ":0", "WAYLAND_DISPLAY" to "wayland-0")))
    }

    @Test
    fun gameModeForwardsDisplayAndSteamIdsAndClearsWayland() {
        val env = mapOf(
            "DISPLAY" to ":1",
            "GAMESCOPE_WAYLAND_DISPLAY" to "gamescope-0",
            "SteamGameId" to "18172863204802166784",
            "SteamAppId" to "4231199437",
            "XAUTHORITY" to "/run/flatpak/Xauthority"
        )
        assertEquals(
            listOf(
                "--env=DISPLAY=:1",
                "--env=GAMESCOPE_WAYLAND_DISPLAY=gamescope-0",
                "--env=SteamGameId=18172863204802166784",
                "--env=SteamAppId=4231199437",
                "--unset-env=WAYLAND_DISPLAY"
            ),
            GameSessionEnvironment.spawnOptions(env)
        )
    }

    @Test
    fun aWaylandDisplayArgosyHasIsKept() {
        val env = mapOf("XDG_CURRENT_DESKTOP" to "gamescope", "WAYLAND_DISPLAY" to "gamescope-0")
        assertEquals(listOf("--env=WAYLAND_DISPLAY=gamescope-0"), GameSessionEnvironment.spawnOptions(env))
    }
}
