package com.nendo.argosy.data.quaypass

import javax.inject.Inject
import javax.inject.Singleton
import java.security.PublicKey

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

    actual data class KeyInfo(
        actual val publicKey: PublicKey,
        actual val publicKeyEncoded: ByteArray,
        actual val backing: KeyBacking,
        actual val algorithm: Algorithm
    )

    actual fun getOrCreateKeyInfo(alias: String): KeyInfo = TODO("desktop: getOrCreateKeyInfo")
}
