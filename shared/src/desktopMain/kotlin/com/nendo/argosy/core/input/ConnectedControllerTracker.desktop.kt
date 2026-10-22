package com.nendo.argosy.core.input

import com.nendo.argosy.platform.PlatformInputDevices
import com.nendo.argosy.util.SafeCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton
import com.nendo.argosy.platform.PlatformInputDevice as InputDevice
import com.nendo.argosy.platform.PlatformInputEvent as InputEvent

@Singleton
actual class ConnectedControllerTracker @Inject constructor() {
    private val scope = SafeCoroutineScope(Dispatchers.IO, "ConnectedControllerTracker")
    private val activeState = ActiveControllerState()
    private val deviceGeneration = MutableStateFlow(0)

    private val _connectedControllerIds = MutableStateFlow(readConnectedControllerIds())
    actual val connectedControllerIds: StateFlow<Set<String>> = _connectedControllerIds.asStateFlow()

    actual val activeController: StateFlow<ActiveController?> = activeState.active

    actual val activeLayout: StateFlow<DetectedLayout?> =
        combine(activeController, deviceGeneration) { active, _ -> detectActive(active).layout }
            .flowOn(Dispatchers.IO)
            .stateIn(scope, SharingStarted.Eagerly, detectActive(null).layout)

    actual fun noteInput(event: InputEvent) = activeState.noteInput(event)

    actual fun holdActiveController(held: Boolean) = activeState.setHeld(held)

    actual fun activeDevice(): InputDevice? = deviceOf(activeController.value)

    actual fun detectActive(): DetectionResult = detectActive(activeController.value)

    actual fun refresh() {
        _connectedControllerIds.value = readConnectedControllerIds()
        activeState.onPadsChanged(connectedPads().map { it.toActiveController() })
        deviceGeneration.value++
    }

    private fun deviceOf(active: ActiveController?): InputDevice? =
        active?.let { PlatformInputDevices.getDevice(it.deviceId) }?.takeIf { it.actsAsGamepad() }

    private fun detectActive(active: ActiveController?): DetectionResult =
        ControllerDetector.detectFor(deviceOf(active))

    private fun connectedPads(): List<InputDevice> =
        PlatformInputDevices.getDeviceIds()
            .toList()
            .mapNotNull { PlatformInputDevices.getDevice(it) }
            .filter { it.actsAsGamepad() }

    private fun readConnectedControllerIds(): Set<String> =
        PlatformInputDevices.getDeviceIds()
            .toList()
            .mapNotNull { PlatformInputDevices.getDevice(it) }
            .filter { it.isPhysicalGamepad() }
            .mapTo(mutableSetOf()) { controllerIdOf(it) }
}
