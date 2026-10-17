package com.nendo.argosy.data.quaypass

expect class QuayPassKeystore {
    enum class KeyBacking { STRONGBOX, TEE, SOFTWARE, UNKNOWN }

    enum class Algorithm { ED25519, EC_P256 }

    companion object {
        fun aliasFor(rommUserId: Long?): String
    }

    fun sign(data: ByteArray, alias: String): ByteArray

    fun signServerVerifiable(data: ByteArray, alias: String): ByteArray

    fun clear(alias: String)
}
