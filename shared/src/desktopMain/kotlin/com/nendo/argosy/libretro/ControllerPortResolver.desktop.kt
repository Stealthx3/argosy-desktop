package com.nendo.argosy.libretro

import com.nendo.argosy.data.local.entity.ControllerOrderEntity
import javax.inject.Inject
import com.nendo.argosy.platform.PlatformInputDevice as InputDevice

actual class ControllerPortResolver @Inject constructor() {
    actual var onPortClaimed: ((controllerId: String, port: Int) -> Unit)? = null

    actual fun setControllerOrder(orders: List<ControllerOrderEntity>) {
    }

    actual fun clearControllerOrder() {
    }

    actual fun setLaunchPad(controllerId: String?, isDocked: Boolean) {
    }

    actual fun releaseDisconnected(connectedControllerIds: Set<String>) {
    }

    actual fun playerOneControllerId(): String? = null

    actual fun getPort(controllerId: String, fallbackControllerNumber: Int): Int = 0

    actual fun hasCustomOrder(): Boolean = false

    actual fun peekPort(device: InputDevice): Int? = null
}
