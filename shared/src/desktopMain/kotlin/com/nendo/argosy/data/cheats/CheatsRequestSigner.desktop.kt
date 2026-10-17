package com.nendo.argosy.data.cheats

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class CheatsRequestSigner @Inject constructor() {
    actual data class SignedRequest(
        actual val deviceToken: String,
        actual val timestamp: Long,
        actual val signature: String,
        actual val fpHash: String
    )

    actual fun sign(deviceToken: String, query: String): SignedRequest? = null

    actual fun isConfigured(): Boolean = false
}
