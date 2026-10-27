package com.nendo.argosy.ui.screens.settings.components

import android.view.InputDevice
import com.nendo.argosy.core.input.controllerIdOf
import com.nendo.argosy.data.repository.InputConfigRepository
import com.nendo.argosy.libretro.ControllerSeating
import com.nendo.argosy.ui.input.GamepadEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SeatHolder(
    val controllerId: String,
    val name: String?
)

data class ControllerSeatState(
    val seats: List<SeatHolder?> = List(ControllerSeating.PORT_COUNT) { null }
) {
    val nextOpenSeat: Int? get() = seats.indexOfFirst { it == null }.takeIf { it >= 0 }
}

fun claimOrRelease(seats: List<SeatHolder?>, controllerId: String, name: String?): List<SeatHolder?> {
    val held = seats.indexOfFirst { it?.controllerId == controllerId }
    if (held >= 0) return seats.toMutableList().also { it[held] = null }
    val open = seats.indexOfFirst { it == null }
    if (open < 0) return seats
    return seats.toMutableList().also { it[open] = SeatHolder(controllerId, name) }
}

/**
 * Switch-style seating: each controller takes the first open seat by pressing a button, and
 * gives it up by pressing again. Each change is written to the saved controller order.
 */
class ControllerSeatController(
    private val repository: InputConfigRepository,
    private val scope: CoroutineScope,
    private val onSeatsChanged: suspend () -> Unit = {}
) {
    private val _state = MutableStateFlow(ControllerSeatState())
    val state: StateFlow<ControllerSeatState> = _state.asStateFlow()
    private val writes = Mutex()
    private var replacesSavedOrder = false

    fun open() {
        replacesSavedOrder = true
        _state.value = ControllerSeatState()
    }

    /**
     * Applies one press from the seat modal. A face or shoulder button claims or releases the
     * pressing pad's seat, Y clears every seat, and Back closes. True when the press closes the
     * modal.
     */
    fun onPadPress(event: GamepadEvent?, pressingController: InputDevice?): Boolean {
        when (event) {
            GamepadEvent.Back -> return true
            GamepadEvent.SecondaryAction -> clearAll()
            GamepadEvent.Confirm,
            GamepadEvent.ContextMenu,
            GamepadEvent.PrevSection,
            GamepadEvent.NextSection -> pressingController?.let { claimOrRelease(controllerIdOf(it), it.name) }
            else -> Unit
        }
        return false
    }

    fun claimOrRelease(controllerId: String, name: String?) {
        val before = _state.value.seats
        val after = claimOrRelease(before, controllerId, name)
        if (after == before) return
        _state.update { it.copy(seats = after) }
        val port = after.indexOfFirst { it?.controllerId == controllerId }.takeIf { it >= 0 }
        persist(controllerId, name ?: controllerId, port)
    }

    fun releaseSeat(port: Int) {
        val holder = _state.value.seats.getOrNull(port) ?: return
        _state.update { state -> state.copy(seats = state.seats.toMutableList().also { it[port] = null }) }
        persist(holder.controllerId, holder.name ?: holder.controllerId, null)
    }

    fun clearAll() {
        replacesSavedOrder = false
        _state.update { state -> state.copy(seats = List(ControllerSeating.PORT_COUNT) { null }) }
        scope.launch {
            writes.withLock {
                repository.clearControllerOrder()
                onSeatsChanged()
            }
        }
    }

    private fun persist(controllerId: String, name: String, port: Int?) {
        val clearFirst = replacesSavedOrder
        replacesSavedOrder = false
        scope.launch {
            writes.withLock {
                if (clearFirst) repository.clearControllerOrder()
                repository.seatController(controllerId, name, port)
                onSeatsChanged()
            }
        }
    }
}
