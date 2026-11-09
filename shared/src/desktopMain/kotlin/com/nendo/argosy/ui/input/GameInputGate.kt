package com.nendo.argosy.ui.input

import com.nendo.argosy.platform.sdl.SdlPadEvent

/**
 * Keeps controller input away from the launcher while a host game runs. Button and axis events
 * are refused during play; when the game ends, they stay refused until every button held at that
 * moment is released (its release included), or [SETTLE_MS] passes. Connection events always pass.
 */
class GameInputGate {
    private val held = HashSet<Pair<Int, Int>>()
    private var playing = false
    private var settleUntil = 0L

    fun gameStarted() {
        playing = true
    }

    fun gameEnded(now: Long) {
        playing = false
        settleUntil = if (held.isEmpty()) 0L else now + SETTLE_MS
    }

    fun accept(event: SdlPadEvent, now: Long): Boolean {
        if (event is SdlPadEvent.Button) {
            val key = event.instanceId to event.button
            if (event.down) held += key else held -= key
        }
        if (event !is SdlPadEvent.Button && event !is SdlPadEvent.Axis) return true
        if (playing) return false
        if (settleUntil == 0L) return true
        if (now >= settleUntil) {
            settleUntil = 0L
            return true
        }
        if (held.isEmpty()) settleUntil = 0L
        return false
    }

    companion object {
        const val SETTLE_MS = 3_000L
    }
}
