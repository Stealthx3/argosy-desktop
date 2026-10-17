package com.nendo.argosy.data.netplay

import java.net.DatagramSocket
import java.net.InetSocketAddress
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

actual class NetplayTransport actual constructor(crypto: PacketCrypto, socket: DatagramSocket, localDirection: PacketCrypto.Direction, scope: CoroutineScope) : AutoCloseable {
    actual data class Incoming(actual val source: InetSocketAddress, actual val packet: NetplayPacket)

    actual companion object {}

    actual val incomingPackets: SharedFlow<Incoming> = kotlinx.coroutines.flow.MutableSharedFlow()

    actual suspend fun send(peer: InetSocketAddress, packet: NetplayPacket) {
    }

    override fun close() {
    }
}
