package com.nendo.argosy.data.netplay

import javax.inject.Inject

actual class PacketCrypto @Inject constructor() {
    actual enum class Direction { HostToGuest, GuestToHost }

    actual companion object {
        actual fun fromMasterKey(masterKey: ByteArray): PacketCrypto = TODO("desktop: fromMasterKey")
    }

    actual fun encrypt(plaintext: ByteArray, direction: Direction): ByteArray = ByteArray(0)

    actual fun decrypt(wire: ByteArray, direction: Direction): ByteArray? = null
}
