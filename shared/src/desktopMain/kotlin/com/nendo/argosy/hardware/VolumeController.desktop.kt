package com.nendo.argosy.hardware

import com.nendo.argosy.platform.HostShell
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The host's default audio output, through PipeWire's `wpctl` or, without it, PulseAudio's
 * `pactl` (both on the host when sandboxed). A muted output reads as 0, and setting a level above
 * 0 unmutes it, as Android's media stream does. Writes run on their own thread, latest level wins.
 */
@Singleton
actual class VolumeController @Inject constructor() {
    actual val maxVolume: Int = STEPS

    private val pending = AtomicReference<Float?>(null)

    private val writer = Executors.newSingleThreadExecutor { task ->
        Thread(task, "host-volume").apply { isDaemon = true }
    }

    actual fun getVolume(): DisplayVolume =
        DisplayVolume(primary = HostVolume.read() ?: 0f, secondary = null)

    actual fun setPrimaryVolume(volume: Float): Boolean {
        if (pending.getAndSet(volume.coerceIn(0f, 1f)) == null) {
            writer.execute { pending.getAndSet(null)?.let(HostVolume::write) }
        }
        return true
    }

    private companion object {
        const val STEPS = 20
    }
}

internal object HostVolume {
    private const val WPCTL_SINK = "@DEFAULT_AUDIO_SINK@"
    private const val PACTL_SINK = "@DEFAULT_SINK@"

    private val wpctlLevel = Regex("""Volume:\s*([0-9.]+)""")
    private val pactlPercent = Regex("""(\d+)%""")

    private val hasWpctl: Boolean by lazy { hostHas("wpctl") }

    fun read(): Float? =
        if (hasWpctl) {
            parseWpctl(HostShell.run(listOf("wpctl", "get-volume", WPCTL_SINK)))
        } else {
            parsePactl(
                HostShell.run(listOf("pactl", "get-sink-volume", PACTL_SINK)),
                HostShell.run(listOf("pactl", "get-sink-mute", PACTL_SINK))
            )
        }

    fun write(level: Float) {
        val unmute = level > 0f
        if (hasWpctl) {
            HostShell.run(listOf("wpctl", "set-volume", WPCTL_SINK, "%.2f".format(Locale.ROOT, level)))
            if (unmute) HostShell.run(listOf("wpctl", "set-mute", WPCTL_SINK, "0"))
        } else {
            HostShell.run(listOf("pactl", "set-sink-volume", PACTL_SINK, "${Math.round(level * 100)}%"))
            if (unmute) HostShell.run(listOf("pactl", "set-sink-mute", PACTL_SINK, "0"))
        }
    }

    fun parseWpctl(output: String?): Float? {
        val text = output ?: return null
        if ("[MUTED]" in text) return 0f
        return wpctlLevel.find(text)?.groupValues?.get(1)?.toFloatOrNull()?.coerceIn(0f, 1f)
    }

    fun parsePactl(volume: String?, mute: String?): Float? {
        if (mute?.contains("yes") == true) return 0f
        val percent = volume?.let { pactlPercent.find(it) }?.groupValues?.get(1)?.toIntOrNull() ?: return null
        return (percent / 100f).coerceIn(0f, 1f)
    }

    private fun hostHas(command: String): Boolean =
        HostShell.runScript("command -v \"$1\" >/dev/null && echo yes", listOf(command))?.trim() == "yes"
}
