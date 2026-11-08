package com.nendo.argosy.platform.audio

import com.nendo.argosy.ui.input.SoundDefaults
import com.nendo.argosy.ui.input.SoundPreset
import com.nendo.argosy.core.input.SoundType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PcmDecoderTest {
    private fun wav(format: Short, channels: Short, rate: Int, bits: Short, data: ByteArray): ByteArray {
        val buffer = ByteBuffer.allocate(44 + data.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + data.size).put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray()).putInt(16).putShort(format).putShort(channels).putInt(rate)
            .putInt(rate * channels * bits / 8).putShort((channels * bits / 8).toShort()).putShort(bits)
        buffer.put("data".toByteArray()).putInt(data.size).put(data)
        return buffer.array()
    }

    @Test
    fun pcm16WavDecodes() {
        val samples = byteArrayOf(1, 2, 3, 4)
        val clip = PcmDecoder.decodeWav(wav(1, 2, 44100, 16, samples))!!
        assertEquals(44100, clip.rate)
        assertEquals(2, clip.channels)
        assertContentEquals(samples, clip.samples)
    }

    @Test
    fun nonPcmOrNon16BitWavIsRejected() {
        assertNull(PcmDecoder.decodeWav(wav(3, 1, 48000, 32, ByteArray(8))))
        assertNull(PcmDecoder.decodeWav(wav(1, 1, 48000, 8, ByteArray(8))))
        assertNull(PcmDecoder.decodeWav("not a wav file at all, long enough to pass".toByteArray()))
    }

    @Test
    fun defaultsMatchTheAndroidTable() {
        assertEquals(SoundPreset.TAP_LIGHT, SoundDefaults.presetFor(SoundType.NAVIGATE))
        assertEquals(SoundPreset.COLLECT, SoundDefaults.presetFor(SoundType.LAUNCH_GAME))
        assertNull(SoundDefaults.presetFor(SoundType.SILENT))
    }
}
