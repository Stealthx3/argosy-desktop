package com.nendo.argosy.core.input

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.nendo.argosy.platform.PlatformInputDevice as InputDevice

@Singleton
actual class ConnectedControllerTracker @Inject constructor() {
    actual val connectedControllerIds: StateFlow<Set<String>> = MutableStateFlow(emptySet())

    actual val activeController: StateFlow<ActiveController?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual val activeLayout: StateFlow<DetectedLayout?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual fun activeDevice(): InputDevice? = null

    actual fun detectActive(): DetectionResult = TODO("desktop: detectActive")

    actual fun refresh() {
    }
}
