package com.nendo.argosy.platform.audio

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.IntByReference
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Signed 16-bit little-endian PCM, interleaved, ready for [PulseOutput].
 */
class PcmClip(val samples: ByteArray, val rate: Int, val channels: Int)

/**
 * Decodes Ogg Vorbis (through the system `libvorbisfile`) and PCM WAV files into [PcmClip]s; null for
 * any other format or a file that does not decode.
 */
object PcmDecoder {
    fun decode(file: File): PcmClip? = when (file.extension.lowercase()) {
        "ogg", "oga" -> decodeVorbis(file)
        "wav" -> decodeWav(file.readBytes())
        else -> null
    }

    private fun decodeVorbis(file: File): PcmClip? {
        val vorbis = VorbisFile.library ?: return null
        val handle = Memory(OGG_VORBIS_FILE_BYTES)
        if (vorbis.ov_fopen(file.absolutePath, handle) != 0) return null
        return try {
            val info = vorbis.ov_info(handle, -1) ?: return null
            val channels = info.getInt(INFO_CHANNELS_OFFSET)
            val rate = info.getLong(INFO_RATE_OFFSET).toInt()
            val out = ByteArrayOutputStream()
            val buffer = Memory(READ_CHUNK.toLong())
            val section = IntByReference()
            while (true) {
                val read = vorbis.ov_read(handle, buffer, READ_CHUNK, 0, 2, 1, section).toInt()
                if (read <= 0) break
                out.write(buffer.getByteArray(0, read))
            }
            if (channels <= 0 || rate <= 0 || out.size() == 0) null else PcmClip(out.toByteArray(), rate, channels)
        } finally {
            vorbis.ov_clear(handle)
        }
    }

    fun decodeWav(bytes: ByteArray): PcmClip? {
        if (bytes.size < WAV_HEADER || String(bytes, 0, 4) != "RIFF" || String(bytes, 8, 4) != "WAVE") return null
        val data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var offset = 12
        var channels = 0
        var rate = 0
        var bits = 0
        var format = 0
        while (offset + 8 <= bytes.size) {
            val id = String(bytes, offset, 4)
            val size = data.getInt(offset + 4)
            val body = offset + 8
            if (size < 0 || body + size > bytes.size) return null
            when (id) {
                "fmt " -> {
                    format = data.getShort(body).toInt()
                    channels = data.getShort(body + 2).toInt()
                    rate = data.getInt(body + 4)
                    bits = data.getShort(body + 14).toInt()
                }
                "data" -> {
                    if (format != WAV_PCM || bits != 16 || channels <= 0 || rate <= 0) return null
                    return PcmClip(bytes.copyOfRange(body, body + size), rate, channels)
                }
            }
            offset = body + size + (size and 1)
        }
        return null
    }

    private const val OGG_VORBIS_FILE_BYTES = 4096L
    private const val INFO_CHANNELS_OFFSET = 4L
    private const val INFO_RATE_OFFSET = 8L
    private const val READ_CHUNK = 16384
    private const val WAV_HEADER = 44
    private const val WAV_PCM = 1
}

@Suppress("FunctionName")
internal interface VorbisFile : Library {
    fun ov_fopen(path: String, handle: Pointer): Int
    fun ov_info(handle: Pointer, link: Int): Pointer?
    fun ov_read(handle: Pointer, buffer: Pointer, length: Int, bigEndian: Int, word: Int, signed: Int, section: IntByReference): Long
    fun ov_clear(handle: Pointer): Int

    companion object {
        val library: VorbisFile? by lazy {
            listOf("vorbisfile", "libvorbisfile.so.3").firstNotNullOfOrNull { name ->
                runCatching { Native.load(name, VorbisFile::class.java) }.getOrNull()
            }
        }
    }
}

/**
 * Plays a [PcmClip] to the default PulseAudio/PipeWire output through the system
 * `libpulse-simple`, blocking until the clip has played.
 */
object PulseOutput {
    val available: Boolean get() = PulseSimple.library != null

    fun play(clip: PcmClip, volume: Float, streamName: String): Boolean {
        val pulse = PulseSimple.library ?: return false
        val spec = PulseSimple.SampleSpec().apply {
            format = SAMPLE_S16LE
            rate = clip.rate
            channels = clip.channels.toByte()
        }
        val error = IntByReference()
        val stream = pulse.pa_simple_new(null, APP_NAME, STREAM_PLAYBACK, null, streamName, spec, null, null, error)
            ?: return false
        return try {
            val scaled = scale(clip.samples, volume)
            val buffer = Memory(scaled.size.toLong()).apply { write(0, scaled, 0, scaled.size) }
            pulse.pa_simple_write(stream, buffer, scaled.size.toLong(), error) == 0 &&
                pulse.pa_simple_drain(stream, error) == 0
        } finally {
            pulse.pa_simple_free(stream)
        }
    }

    private fun scale(samples: ByteArray, volume: Float): ByteArray {
        if (volume >= 1f) return samples
        val out = samples.copyOf()
        var i = 0
        while (i + 1 < out.size) {
            val value = ((out[i + 1].toInt() shl 8) or (out[i].toInt() and 0xFF)).toShort()
            val scaled = (value * volume).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            out[i] = scaled.toByte()
            out[i + 1] = (scaled shr 8).toByte()
            i += 2
        }
        return out
    }

    private const val APP_NAME = "Argosy"
    private const val STREAM_PLAYBACK = 1
    private const val SAMPLE_S16LE = 3
}

@Suppress("FunctionName")
internal interface PulseSimple : Library {
    fun pa_simple_new(
        server: String?,
        name: String,
        direction: Int,
        device: String?,
        streamName: String,
        spec: SampleSpec,
        map: Pointer?,
        attributes: Pointer?,
        error: IntByReference
    ): Pointer?

    fun pa_simple_write(stream: Pointer, data: Pointer, bytes: Long, error: IntByReference): Int
    fun pa_simple_drain(stream: Pointer, error: IntByReference): Int
    fun pa_simple_free(stream: Pointer)

    @Structure.FieldOrder("format", "rate", "channels")
    class SampleSpec : Structure() {
        @JvmField var format: Int = 0
        @JvmField var rate: Int = 0
        @JvmField var channels: Byte = 0
    }

    companion object {
        val library: PulseSimple? by lazy {
            listOf("pulse-simple", "libpulse-simple.so.0").firstNotNullOfOrNull { name ->
                runCatching { Native.load(name, PulseSimple::class.java) }.getOrNull()
            }
        }
    }
}
