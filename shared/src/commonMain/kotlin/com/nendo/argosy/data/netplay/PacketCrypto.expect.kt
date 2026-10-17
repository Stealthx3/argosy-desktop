package com.nendo.argosy.data.netplay

expect class PacketCrypto : AutoCloseable {
    enum class Direction { HostToGuest, GuestToHost }

    companion object {
        fun fromMasterKey(masterKey: ByteArray): PacketCrypto
    }

    fun encrypt(plaintext: ByteArray, direction: Direction): ByteArray

    fun decrypt(wire: ByteArray, direction: Direction): ByteArray?
}
