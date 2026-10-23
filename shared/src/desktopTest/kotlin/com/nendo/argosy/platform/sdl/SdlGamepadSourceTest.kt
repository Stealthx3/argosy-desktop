package com.nendo.argosy.platform.sdl

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SdlGamepadSourceTest {
    @Test
    fun virtualPadReachesTheSourceAsGamepadEventsAndTakesRumble() {
        val sdl = Sdl3.load()
        if (sdl == null) {
            println("SDL3 not installed; skipped")
            return
        }
        val events = LinkedBlockingQueue<SdlPadEvent>()
        val rumbles = LinkedBlockingQueue<Pair<Int, Int>>()
        val source = SdlGamepadSource(sdl) { events.put(it) }
        source.start()
        assertTrue(source.awaitReady(5_000), "SDL_Init")
        val pad = assertNotNull(
            SdlVirtualPad.attach(sdl, "Test Pad", vendorId = 0x045e, productId = 0x028e) { low, high -> rumbles.put(low to high) }
        )
        try {
            val added = next(events) { it is SdlPadEvent.Added && it.instanceId == pad.instanceId } as SdlPadEvent.Added
            assertEquals("Test Pad", added.name)
            assertEquals(0x045e, added.vendorId)
            assertEquals(0x028e, added.productId)
            assertTrue(added.descriptor.isNotEmpty())

            assertTrue(pad.setButton(Sdl3.BUTTON_SOUTH, true))
            assertEquals(SdlPadEvent.Button(pad.instanceId, Sdl3.BUTTON_SOUTH, true), next(events) { it is SdlPadEvent.Button })
            assertTrue(pad.setButton(Sdl3.BUTTON_SOUTH, false))
            assertEquals(SdlPadEvent.Button(pad.instanceId, Sdl3.BUTTON_SOUTH, false), next(events) { it is SdlPadEvent.Button })

            assertTrue(pad.setAxis(Sdl3.AXIS_LEFTY, Short.MAX_VALUE))
            assertEquals(SdlPadEvent.Axis(pad.instanceId, Sdl3.AXIS_LEFTY, Short.MAX_VALUE), next(events) { it is SdlPadEvent.Axis && it.axis == Sdl3.AXIS_LEFTY })

            assertTrue(source.rumble(0xc000, 0x4000, 50))
            assertEquals(0xc000 to 0x4000, rumbles.poll(5, TimeUnit.SECONDS))
        } finally {
            pad.detach()
        }
        assertEquals(SdlPadEvent.Removed(pad.instanceId), next(events) { it is SdlPadEvent.Removed })
        source.stop()
    }

    private fun next(events: LinkedBlockingQueue<SdlPadEvent>, match: (SdlPadEvent) -> Boolean): SdlPadEvent {
        while (true) {
            val event = assertNotNull(events.poll(5, TimeUnit.SECONDS), "no matching SDL event")
            if (match(event)) return event
        }
    }
}
