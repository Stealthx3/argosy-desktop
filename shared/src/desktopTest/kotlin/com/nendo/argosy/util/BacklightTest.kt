package com.nendo.argosy.util

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BacklightTest {
    private fun device(root: File, name: String, type: String, max: String, current: String) {
        File(root, name).apply {
            mkdirs()
            File(this, "type").writeText("$type\n")
            File(this, "max_brightness").writeText("$max\n")
            File(this, "brightness").writeText("$current\n")
        }
    }

    @Test
    fun noBacklightDeviceMeansNoBrightness() {
        val root = createTempDirectory("backlight").toFile()
        assertNull(SystemSettings.Backlight.find(root))
        assertNull(SystemSettings.Backlight.find(File(root, "missing")))
    }

    @Test
    fun firmwareDeviceIsPreferredOverRaw() {
        val root = createTempDirectory("backlight").toFile()
        device(root, "acpi_video0", "raw", "100", "40")
        device(root, "amdgpu_bl1", "firmware", "255", "128")
        val backlight = SystemSettings.Backlight.find(root)
        assertEquals("amdgpu_bl1", backlight?.name)
        assertEquals(255, backlight?.maxBrightness)
        assertEquals(128, backlight?.read())
    }

    @Test
    fun deviceWithoutMaximumIsSkipped() {
        val root = createTempDirectory("backlight").toFile()
        device(root, "broken", "firmware", "0", "0")
        device(root, "intel_backlight", "raw", "19200", "9600")
        assertEquals("intel_backlight", SystemSettings.Backlight.find(root)?.name)
    }
}
