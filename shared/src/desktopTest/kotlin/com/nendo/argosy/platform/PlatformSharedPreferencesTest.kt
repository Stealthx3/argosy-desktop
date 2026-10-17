package com.nendo.argosy.platform

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlatformSharedPreferencesTest {
    private fun context() = DesktopPlatformContext(platformFilesDir = Files.createTempDirectory("prefs").toFile())

    @Test
    fun valuesSurviveReopening() {
        val ctx = context()
        ctx.getSharedPreferences("round_trip", MODE_PRIVATE).edit()
            .putString("s", "text")
            .putInt("i", 7)
            .putLong("l", 1L shl 40)
            .putBoolean("b", true)
            .commit()
        val reopened = ctx.filesDir.resolve("shared_prefs/round_trip.properties")
        assertTrue(reopened.exists())
        val again = DesktopPlatformContext(platformFilesDir = ctx.filesDir).getSharedPreferences("round_trip_other", MODE_PRIVATE)
        assertFalse(again.contains("s"))
        val prefs = ctx.getSharedPreferences("round_trip", MODE_PRIVATE)
        assertEquals("text", prefs.getString("s", null))
        assertEquals(7, prefs.getInt("i", 0))
        assertEquals(1L shl 40, prefs.getLong("l", 0L))
        assertTrue(prefs.getBoolean("b", false))
    }

    @Test
    fun removeAndClear() {
        val prefs = context().getSharedPreferences("edits", MODE_PRIVATE)
        prefs.edit().putString("a", "1").putString("b", "2").apply()
        prefs.edit().remove("a").apply()
        assertNull(prefs.getString("a", null))
        assertEquals("2", prefs.getString("b", null))
        prefs.edit().clear().putInt("c", 3).commit()
        assertFalse(prefs.contains("b"))
        assertEquals(3, prefs.getInt("c", 0))
    }
}
