package com.nendo.argosy.data.netplay

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.net.InetSocketAddress

expect class NetplayTransport {
    class Incoming {
        val source: InetSocketAddress
        val packet: NetplayPacket
    }

    companion object {}

    val incomingPackets: SharedFlow<Incoming>

    suspend fun send(peer: InetSocketAddress, packet: NetplayPacket)
}
