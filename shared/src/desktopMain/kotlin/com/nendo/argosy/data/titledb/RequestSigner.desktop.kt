package com.nendo.argosy.data.titledb

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class RequestSigner @Inject constructor() {
    actual fun isConfigured(): Boolean = false

    actual data class SignedRequest(
        actual val deviceToken: String,
        actual val timestamp: Long,
        actual val signature: String,
        actual val fpHash: String
    )

    actual fun sign(deviceToken: String, query: String): SignedRequest? = null
}
