package com.nendo.argosy.data.quaypass

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class QuayPassKeystore @Inject constructor() {
    actual enum class KeyBacking { STRONGBOX, TEE, SOFTWARE, UNKNOWN }

    actual enum class Algorithm { ED25519, EC_P256 }

    actual fun sign(data: ByteArray, alias: String): ByteArray = ByteArray(0)

    actual fun signServerVerifiable(data: ByteArray, alias: String): ByteArray = ByteArray(0)

    actual fun clear(alias: String) {
    }

    actual companion object {
        actual fun aliasFor(rommUserId: Long?): String = ""
    }
}
