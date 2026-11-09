package com.nendo.argosy.ui.input

import com.nendo.argosy.platform.sdl.SdlPadEvent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameInputGateTest {
    private fun down(button: Int) = SdlPadEvent.Button(1, button, true)
    private fun up(button: Int) = SdlPadEvent.Button(1, button, false)

    @Test
    fun inputPassesWithNoGame() {
        val gate = GameInputGate()
        assertTrue(gate.accept(down(0), 0))
        assertTrue(gate.accept(up(0), 10))
        assertTrue(gate.accept(SdlPadEvent.Axis(1, 0, 100), 20))
    }

    @Test
    fun inputDuringPlayIsRefused() {
        val gate = GameInputGate()
        gate.gameStarted()
        assertFalse(gate.accept(down(0), 0))
        assertFalse(gate.accept(up(0), 10))
        assertFalse(gate.accept(SdlPadEvent.Axis(1, 0, 100), 20))
        gate.gameEnded(30)
        assertTrue(gate.accept(down(1), 40))
    }

    @Test
    fun aComboHeldAtGameEndIsSwallowedUntilReleased() {
        val gate = GameInputGate()
        gate.gameStarted()
        gate.accept(down(4), 0)
        gate.accept(down(6), 10)
        gate.gameEnded(100)
        assertFalse(gate.accept(up(4), 200))
        assertFalse(gate.accept(up(6), 300))
        assertTrue(gate.accept(down(0), 400))
    }

    @Test
    fun aStuckButtonStopsBlockingAfterTheSettleTime() {
        val gate = GameInputGate()
        gate.gameStarted()
        gate.accept(down(4), 0)
        gate.gameEnded(100)
        assertFalse(gate.accept(down(0), 200))
        assertTrue(gate.accept(down(0), 100 + GameInputGate.SETTLE_MS))
    }
}
