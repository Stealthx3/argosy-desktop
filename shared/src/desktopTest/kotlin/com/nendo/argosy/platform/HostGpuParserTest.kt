package com.nendo.argosy.platform

import com.nendo.argosy.data.model.HostDisplay
import com.nendo.argosy.data.model.HostGpuType
import com.nendo.argosy.ui.screens.settings.sections.hostGpuRows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HostGpuParserTest {
    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResource("/hostgpu/$name")).readText()

    @Test
    fun nvidiaProprietaryDriverIsReadFromVulkanGlxAndSysfs() {
        val info = HostGpuParser.combine(
            vulkan = fixture("vulkaninfo-nvidia.txt"),
            glx = fixture("glxinfo-nvidia.txt"),
            cards = listOf(HostGpuParser.DrmCard("0x10de", "0x2684", "nvidia")),
            displays = emptyList(),
        )
        assertEquals(1, info.gpus.size)
        val gpu = info.gpus.single()
        assertEquals("NVIDIA GeForce RTX 4090", gpu.name)
        assertEquals("NVIDIA", gpu.vendor)
        assertEquals(HostGpuType.DISCRETE, gpu.type)
        assertEquals("nvidia", gpu.kernelDriver)
        assertEquals("595.99.02", gpu.driver)
        assertEquals(24564, gpu.vramMb)
        assertEquals("1.4.329", gpu.vulkanVersion)
        assertEquals("4.6.0 NVIDIA 595.99.02", gpu.openGlVersion)
    }

    @Test
    fun mesaRendererMatchesTheRadvDevice() {
        val info = HostGpuParser.combine(
            vulkan = fixture("vulkaninfo-amd.txt"),
            glx = fixture("glxinfo-amd.txt"),
            cards = listOf(HostGpuParser.DrmCard("0x1002", "0x747e", "amdgpu")),
            displays = emptyList(),
        )
        val gpu = info.gpus.single()
        assertEquals("AMD", gpu.vendor)
        assertEquals("amdgpu", gpu.kernelDriver)
        assertEquals("Mesa 25.0.2", gpu.driver)
        assertEquals(16384, gpu.vramMb)
        assertEquals("4.6 (Core Profile) Mesa 25.0.2", gpu.openGlVersion)
    }

    @Test
    fun withoutVulkanEachCardIsAGpu() {
        val info = HostGpuParser.combine(
            vulkan = "",
            glx = "",
            cards = listOf(HostGpuParser.DrmCard("0x8086", "0x46a6", "i915")),
            displays = emptyList(),
        )
        val gpu = info.gpus.single()
        assertEquals("Intel", gpu.vendor)
        assertEquals("i915", gpu.kernelDriver)
        assertNull(gpu.vulkanVersion)
    }

    @Test
    fun rowsListEveryGpuFieldAndEachDisplay() {
        val info = HostGpuParser.combine(
            vulkan = fixture("vulkaninfo-nvidia.txt"),
            glx = fixture("glxinfo-nvidia.txt"),
            cards = emptyList(),
            displays = listOf(HostDisplay("DP-1", 3440, 1440, 34.2), HostDisplay("HDMI-A-1", null, null, null)),
        )
        val rows = hostGpuRows(info)
        assertEquals(7 + 2, rows.count { it.focusable })
        assertTrue(rows.first() !is com.nendo.argosy.ui.screens.settings.sections.HostGpuRow.Info)
    }
}
