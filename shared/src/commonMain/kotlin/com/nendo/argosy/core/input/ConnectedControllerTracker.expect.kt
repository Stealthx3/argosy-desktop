package com.nendo.argosy.core.input

import com.nendo.argosy.platform.PlatformInputDevice as InputDevice
import com.nendo.argosy.platform.controllerNumber
import com.nendo.argosy.platform.descriptor
import com.nendo.argosy.platform.id
import com.nendo.argosy.platform.isEnabled
import com.nendo.argosy.platform.isExternal
import com.nendo.argosy.platform.isVirtual
import com.nendo.argosy.platform.name
import com.nendo.argosy.platform.productId
import com.nendo.argosy.platform.sources
import com.nendo.argosy.platform.vendorId
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
