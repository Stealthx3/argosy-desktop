package com.nendo.argosy.hardware

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HostVolumeTest {
    @Test
    fun wpctlLevelIsRead() {
        assertEquals(0.85f, HostVolume.parseWpctl("Volume: 0.85\n"))
    }

    @Test
    fun wpctlMutedReadsAsZero() {
        assertEquals(0f, HostVolume.parseWpctl("Volume: 0.40 [MUTED]\n"))
    }

    @Test
    fun wpctlBoostIsClamped() {
        assertEquals(1f, HostVolume.parseWpctl("Volume: 1.30\n"))
    }

    @Test
    fun wpctlWithoutOutputIsUnknown() {
        assertNull(HostVolume.parseWpctl(null))
        assertNull(HostVolume.parseWpctl("Translate ID error: '-1' is not a valid ID\n"))
    }

    @Test
    fun pactlFirstChannelPercentIsRead() {
        val volume = "Volume: front-left: 42598 /  65% / -11.23 dB,   front-right: 42598 /  65% / -11.23 dB\n"
        assertEquals(0.65f, HostVolume.parsePactl(volume, "Mute: no\n"))
    }

    @Test
    fun pactlMutedReadsAsZero() {
        assertEquals(0f, HostVolume.parsePactl("Volume: front-left: 42598 /  65% / -11.23 dB\n", "Mute: yes\n"))
    }
}
