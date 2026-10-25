package com.nendo.argosy.core.input

import android.view.MotionEvent
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ControllerPointerGuardTest {

    private var live = true
    private val touchableCalls = mutableListOf<Boolean>()
    private val controllerPointer = mockk<MotionEvent>()
    private val otherEvent = mockk<MotionEvent>()

    private fun TestScope.guard() = ControllerPointerGuard(
        windowName = "launcher",
        scope = backgroundScope,
        sessionLive = { live },
        setWindowTouchable = { touchableCalls += it },
        isControllerPointer = { it === controllerPointer }
    )

    @Test
    fun `a controller pointer during a live session over a window not in front lets pointers pass through`() = runTest {
        val guard = guard()

        assertTrue(guard.consumes(controllerPointer))

        assertEquals(listOf(false), touchableCalls)
    }

    @Test
    fun `the window takes pointer input again once the pass-through time is over`() = runTest {
        val guard = guard()
        guard.consumes(controllerPointer)

        advanceTimeBy(ControllerPointerGuard.PASS_THROUGH_MS - 1)
        runCurrent()
        assertEquals(listOf(false), touchableCalls)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(false, true), touchableCalls)
    }

    @Test
    fun `a controller pointer after the pass-through time starts it again`() = runTest {
        val guard = guard()
        guard.consumes(controllerPointer)
        advanceTimeBy(ControllerPointerGuard.PASS_THROUGH_MS)
        runCurrent()

        guard.consumes(controllerPointer)

        assertEquals(listOf(false, true, false), touchableCalls)
    }

    @Test
    fun `a controller pointer over the window in front is consumed but never passes through`() = runTest {
        val guard = guard()
        guard.onInFrontChanged(true)

        assertTrue(guard.consumes(controllerPointer))

        assertTrue(touchableCalls.isEmpty())
    }

    @Test
    fun `other events and events outside a live session pass through untouched`() = runTest {
        val guard = guard()

        assertFalse(guard.consumes(otherEvent))
        live = false
        assertFalse(guard.consumes(controllerPointer))

        assertTrue(touchableCalls.isEmpty())
    }

    @Test
    fun `coming to the front makes the window touchable at once and cancels the timer`() = runTest {
        val guard = guard()
        guard.consumes(controllerPointer)

        guard.onInFrontChanged(true)
        advanceTimeBy(ControllerPointerGuard.PASS_THROUGH_MS * 2)
        runCurrent()

        assertEquals(listOf(false, true), touchableCalls)
    }

    @Test
    fun `release restores touch once and repeated pointers do not re-apply the flag`() = runTest {
        val guard = guard()
        guard.consumes(controllerPointer)
        guard.consumes(controllerPointer)

        guard.release()
        guard.release()

        assertEquals(listOf(false, true), touchableCalls)
    }

    @Test
    fun `after the session ends a controller pointer cannot start a pass-through`() = runTest {
        val guard = guard()
        guard.consumes(controllerPointer)
        live = false
        guard.release()

        assertFalse(guard.consumes(controllerPointer))

        assertEquals(listOf(false, true), touchableCalls)
    }

    @Test
    fun `leaving the front does not pass pointers through until a controller pointer arrives`() = runTest {
        val guard = guard()
        guard.onInFrontChanged(true)
        guard.onInFrontChanged(false)

        assertTrue(touchableCalls.isEmpty())
        assertTrue(guard.consumes(controllerPointer))
        assertEquals(listOf(false), touchableCalls)
    }
}
