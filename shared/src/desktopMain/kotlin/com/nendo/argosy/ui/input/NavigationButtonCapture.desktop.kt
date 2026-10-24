package com.nendo.argosy.ui.input

import com.nendo.argosy.core.input.ConnectedControllerTracker
import com.nendo.argosy.platform.descriptor
import com.nendo.argosy.platform.name
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class NavigationButtonCapture @Inject constructor(
    private val remapper: NavigationButtonRemapper,
    private val controllerTracker: ConnectedControllerTracker,
) {
    actual val supported: Boolean = true

    actual fun activeController(): MappedController? = controllerTracker.activeDevice()?.let {
        MappedController(NavigationButtonRemapper.controllerKey(it.descriptor), it.name)
    }

    actual fun captureNext(listener: (controller: String, keyCode: Int) -> Unit) = remapper.captureNext(listener)

    actual fun cancel() = remapper.cancelCapture()
}
