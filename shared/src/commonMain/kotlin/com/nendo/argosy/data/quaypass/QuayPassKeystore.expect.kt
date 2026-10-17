package com.nendo.argosy.data.quaypass

import java.security.PublicKey

expect class QuayPassKeystore {
    enum class KeyBacking { STRONGBOX, TEE, SOFTWARE, UNKNOWN }

    enum class Algorithm { ED25519, EC_P256 }

    class KeyInfo {
        val publicKey: PublicKey
        val publicKeyEncoded: ByteArray
        val backing: KeyBacking
        val algorithm: Algorithm
    }

    companion object {
        fun aliasFor(rommUserId: Long?): String
    }

    fun getOrCreateKeyInfo(alias: String): KeyInfo

    fun sign(data: ByteArray, alias: String): ByteArray

    fun signServerVerifiable(data: ByteArray, alias: String): ByteArray

    fun clear(alias: String)
}
