package com.nendo.argosy.libretro

import com.nendo.argosy.platform.PlatformInputDevice as InputDevice
import com.nendo.argosy.data.local.entity.ControllerOrderEntity

expect class ControllerPortResolver {
    var onPortClaimed: ((controllerId: String, port: Int) -> Unit)?

    fun setControllerOrder(orders: List<ControllerOrderEntity>)

    fun clearControllerOrder()

    fun setLaunchPad(controllerId: String?, isDocked: Boolean)

    fun releaseDisconnected(connectedControllerIds: Set<String>)

    fun playerOneControllerId(): String?

    fun peekPort(device: InputDevice): Int?

    fun getPort(controllerId: String, fallbackControllerNumber: Int): Int

    fun hasCustomOrder(): Boolean
}
