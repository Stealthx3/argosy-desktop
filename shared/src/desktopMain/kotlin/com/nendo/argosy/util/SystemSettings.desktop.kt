package com.nendo.argosy.util

import com.nendo.argosy.platform.HostShell
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * The host's built-in panel backlight: read from `/sys/class/backlight`, written through logind's
 * `Session.SetBrightness`, which the active session may call without root. A machine without a
 * backlight device (a desktop with external monitors) reports no brightness.
 */
@Singleton
actual class SystemSettings @Inject constructor() {
    private val backlight: Backlight? by lazy { Backlight.find(File(BACKLIGHT_ROOT)) }

    actual fun screenBrightness(): Float? {
        val device = backlight ?: return null
        val current = device.read() ?: return null
        return (current.toFloat() / device.maxBrightness).coerceIn(0f, 1f)
    }

    actual fun setScreenBrightness(fraction: Float): Boolean {
        val device = backlight ?: return false
        val value = (fraction.coerceIn(0f, 1f) * device.maxBrightness).roundToInt()
            .coerceIn(device.minimumValue, device.maxBrightness)
        HostShell.run(
            listOf(
                "busctl", "call", "org.freedesktop.login1", "/org/freedesktop/login1/session/auto",
                "org.freedesktop.login1.Session", "SetBrightness", "ssu", "backlight", device.name, value.toString()
            )
        )
        return device.read() == value
    }

    internal class Backlight(val name: String, val maxBrightness: Int, private val brightnessFile: File) {
        val minimumValue: Int get() = (maxBrightness / MINIMUM_DIVISOR).coerceAtLeast(1)

        fun read(): Int? = readInt(brightnessFile)

        companion object {
            private const val MINIMUM_DIVISOR = 100
            private val typeOrder = listOf("firmware", "platform", "raw")

            fun find(root: File): Backlight? = root.listFiles().orEmpty()
                .sortedWith(compareBy<File>({ typeRank(it) }, { it.name }))
                .firstNotNullOfOrNull { device ->
                    val max = readInt(File(device, "max_brightness"))?.takeIf { it > 0 }
                    max?.let { Backlight(device.name, it, File(device, "brightness")) }
                }

            private fun typeRank(device: File): Int {
                val type = runCatching { File(device, "type").readText().trim() }.getOrNull()
                return typeOrder.indexOf(type).takeIf { it >= 0 } ?: typeOrder.size
            }

            private fun readInt(file: File): Int? = runCatching { file.readText().trim().toInt() }.getOrNull()
        }
    }

    private companion object {
        const val BACKLIGHT_ROOT = "/sys/class/backlight"
    }
}
