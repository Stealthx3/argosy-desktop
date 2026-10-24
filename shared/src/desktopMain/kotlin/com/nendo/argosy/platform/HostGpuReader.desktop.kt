package com.nendo.argosy.platform

import com.nendo.argosy.data.model.HostDisplay
import com.nendo.argosy.data.model.HostGpu
import com.nendo.argosy.data.model.HostGpuInfo
import com.nendo.argosy.data.model.HostGpuType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class HostGpuReader @Inject constructor() {
    actual val isSupported: Boolean = true

    actual suspend fun read(): HostGpuInfo = withContext(Dispatchers.IO) {
        HostGpuParser.combine(
            vulkan = HostShell.run(listOf("vulkaninfo", "--summary")).orEmpty(),
            glx = HostShell.run(listOf("glxinfo", "-B")).orEmpty(),
            cards = drmCards(),
            displays = drmDisplays(),
        )
    }

    private fun drmCards(): List<HostGpuParser.DrmCard> =
        File(DRM_DIR).listFiles().orEmpty()
            .filter { CARD.matches(it.name) }
            .map { card ->
                HostGpuParser.DrmCard(
                    vendorId = File(card, "device/vendor").readTrimmed(),
                    deviceId = File(card, "device/device").readTrimmed(),
                    driver = runCatching { File(card, "device/driver").canonicalFile.name }.getOrNull(),
                )
            }

    private fun drmDisplays(): List<HostDisplay> =
        File(DRM_DIR).listFiles().orEmpty()
            .filter { CONNECTOR.matches(it.name) && File(it, "status").readTrimmed() == "connected" }
            .sortedBy { it.name }
            .map { connector ->
                val mode = File(connector, "modes").readTrimmed()?.lineSequence()?.firstOrNull()
                val size = mode?.split('x')?.mapNotNull { it.takeWhile(Char::isDigit).toIntOrNull() }
                val edid = runCatching { File(connector, "edid").readBytes() }.getOrNull()
                HostDisplay(
                    connector = connector.name.substringAfter('-'),
                    width = size?.getOrNull(0),
                    height = size?.getOrNull(1),
                    diagonalInches = edid?.let(DisplayProfileDetection::diagonalInches),
                )
            }

    private fun File.readTrimmed(): String? = runCatching { readText().trim() }.getOrNull()

    private companion object {
        const val DRM_DIR = "/sys/class/drm"
        val CARD = Regex("card\\d+")
        val CONNECTOR = Regex("card\\d+-.+")
    }
}

/**
 * Builds [HostGpuInfo] from `vulkaninfo --summary`, `glxinfo -B` and the DRM cards in sysfs. Each
 * Vulkan device that is not a CPU renderer becomes a GPU; OpenGL details attach to the GPU whose
 * name the OpenGL renderer string starts with, and the kernel driver to the card with the same
 * PCI vendor and device ids. Without Vulkan, each DRM card becomes a GPU named by its ids.
 */
object HostGpuParser {
    data class DrmCard(val vendorId: String?, val deviceId: String?, val driver: String?)

    private data class GlInfo(val renderer: String?, val version: String?, val vramMb: Int?)

    fun combine(vulkan: String, glx: String, cards: List<DrmCard>, displays: List<HostDisplay>): HostGpuInfo {
        val gl = parseGlx(glx)
        val fromVulkan = parseVulkan(vulkan)
            .filter { it["deviceType"] != "PHYSICAL_DEVICE_TYPE_CPU" }
            .map { device ->
                val name = device["deviceName"].orEmpty()
                val vendorId = device["vendorID"]?.lowercase()
                val deviceId = device["deviceID"]?.lowercase()
                val card = cards.firstOrNull { it.vendorId?.lowercase() == vendorId && it.deviceId?.lowercase() == deviceId }
                val glMatches = gl.renderer != null && rendererKey(gl.renderer).let { key -> key.isNotEmpty() && name.startsWith(key) }
                HostGpu(
                    name = name,
                    vendor = vendorName(vendorId),
                    type = gpuType(device["deviceType"]),
                    kernelDriver = card?.driver,
                    driver = device["driverInfo"] ?: device["driverName"],
                    vramMb = if (glMatches) gl.vramMb else null,
                    vulkanVersion = device["apiVersion"],
                    openGlVersion = if (glMatches) gl.version else null,
                )
            }
        val gpus = fromVulkan.ifEmpty {
            cards.map { card ->
                val name = gl.renderer?.substringBefore('/')?.trim()
                    ?: listOfNotNull(vendorName(card.vendorId?.lowercase()), card.deviceId).joinToString(" ")
                HostGpu(name, vendorName(card.vendorId?.lowercase()), HostGpuType.OTHER, card.driver, null, gl.vramMb, null, gl.version)
            }
        }
        return HostGpuInfo(gpus = gpus, displays = displays)
    }

    private fun parseVulkan(output: String): List<Map<String, String>> {
        val devices = mutableListOf<MutableMap<String, String>>()
        output.lineSequence().map { it.trim() }.forEach { line ->
            when {
                GPU_HEADER.matches(line) -> devices += linkedMapOf()
                '=' in line && devices.isNotEmpty() ->
                    devices.last()[line.substringBefore('=').trim()] = line.substringAfter('=').trim()
            }
        }
        return devices
    }

    private fun parseGlx(output: String): GlInfo {
        val lines = output.lineSequence().map { it.trim() }.toList()
        fun value(prefix: String) = lines.firstOrNull { it.startsWith(prefix) }?.substringAfter(':')?.trim()
        val vram = (value("Dedicated video memory") ?: value("Video memory"))
            ?.takeWhile { it.isDigit() }?.toIntOrNull()
        return GlInfo(
            renderer = value("OpenGL renderer string"),
            version = value("OpenGL core profile version string") ?: value("OpenGL version string"),
            vramMb = vram,
        )
    }

    private fun rendererKey(renderer: String): String = renderer.substringBefore('/').substringBefore(" (").trim()

    private fun gpuType(value: String?): HostGpuType = when (value) {
        "PHYSICAL_DEVICE_TYPE_DISCRETE_GPU" -> HostGpuType.DISCRETE
        "PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU" -> HostGpuType.INTEGRATED
        "PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU" -> HostGpuType.VIRTUAL
        else -> HostGpuType.OTHER
    }

    private fun vendorName(id: String?): String? = when (id) {
        "0x10de" -> "NVIDIA"
        "0x1002" -> "AMD"
        "0x8086" -> "Intel"
        null -> null
        else -> id
    }

    private val GPU_HEADER = Regex("GPU\\d+:")
}
