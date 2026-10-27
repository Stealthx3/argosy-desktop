package com.nendo.argosy.ui.screens.settings.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ClaimOrReleaseTest {

    private val empty: List<SeatHolder?> = List(4) { null }

    @Test
    fun `seats fill in the order controllers press`() {
        val first = claimOrRelease(empty, "pad-b", "B")
        val second = claimOrRelease(first, "pad-a", "A")

        assertEquals(listOf(SeatHolder("pad-b", "B"), SeatHolder("pad-a", "A"), null, null), second)
    }

    @Test
    fun `pressing again gives the seat up and the next press takes it`() {
        val seated = claimOrRelease(claimOrRelease(empty, "pad-a", "A"), "pad-b", "B")
        val released = claimOrRelease(seated, "pad-a", "A")
        val reclaimed = claimOrRelease(released, "pad-c", "C")

        assertEquals(listOf(null, SeatHolder("pad-b", "B"), null, null), released)
        assertEquals(listOf(SeatHolder("pad-c", "C"), SeatHolder("pad-b", "B"), null, null), reclaimed)
    }

    @Test
    fun `a fifth controller finds no seat`() {
        val full = listOf("a", "b", "c", "d").fold(empty) { seats, id -> claimOrRelease(seats, id, id) }

        assertEquals(full, claimOrRelease(full, "e", "e"))
    }
}
