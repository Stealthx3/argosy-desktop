package com.nendo.argosy.data.emulator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlathubSuggestionsTest {
    private val entries = LinuxEmulatorDatabase.bundled().emulators

    @Test
    fun suggestionsAreFlathubEmulatorsForThePlatformThatAreNotInstalled() {
        val gba = FlathubSuggestions.forPlatform(entries, "gba", installedPackages = emptySet())
        assertTrue(gba.any { it.displayName == "mGBA" }, gba.map { it.displayName }.toString())
        assertTrue(gba.any { it.displayName == "RetroArch" })
        assertTrue(gba.all { it.downloadUrl!!.startsWith(FlathubSuggestions.PAGE) && it.releaseSource == null })
        val retroarch = EmulatorRegistry.getById("retroarch")!!.packageName
        val withoutRetroArch = FlathubSuggestions.forPlatform(entries, "gba", setOf(retroarch))
        assertTrue(withoutRetroArch.none { it.displayName == "RetroArch" })
    }

    @Test
    fun anEmulatorWithoutAFlathubIdIsNotSuggested() {
        val vita = FlathubSuggestions.forPlatform(entries, "psvita", emptySet())
        assertTrue(vita.none { it.id == "vita3k" })
    }

    @Test
    fun installUsesTheUserFlathubRemote() {
        assertEquals(
            listOf("flatpak", "install", "--user", "-y", "--noninteractive", "flathub", "io.mgba.mGBA"),
            FlathubSuggestions.installArgs("io.mgba.mGBA")
        )
        assertTrue("--if-not-exists" in FlathubSuggestions.ADD_REMOTE)
    }
}
