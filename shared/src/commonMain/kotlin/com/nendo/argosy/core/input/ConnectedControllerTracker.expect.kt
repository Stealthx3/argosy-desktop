package com.nendo.argosy.core.input

import com.nendo.argosy.platform.PlatformInputDevice as InputDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

expect class ConnectedControllerTracker {
    val connectedControllerIds: StateFlow<Set<String>>

    val activeController: StateFlow<ActiveController?>

    val activeLayout: StateFlow<DetectedLayout?>

    fun activeDevice(): InputDevice?

    fun detectActive(): DetectionResult

    fun refresh()
}
