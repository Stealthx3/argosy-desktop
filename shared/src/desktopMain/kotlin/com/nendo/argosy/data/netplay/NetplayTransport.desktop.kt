package com.nendo.argosy.data.netplay

import java.net.InetSocketAddress
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

actual class NetplayTransport @Inject constructor() {
    actual data class Incoming(actual val source: InetSocketAddress, actual val packet: NetplayPacket)

    actual companion object {}

    actual val incomingPackets: SharedFlow<Incoming> = kotlinx.coroutines.flow.MutableSharedFlow()

    actual suspend fun send(peer: InetSocketAddress, packet: NetplayPacket) {
    }
}
