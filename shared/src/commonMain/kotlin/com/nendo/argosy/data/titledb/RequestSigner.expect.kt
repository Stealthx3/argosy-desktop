package com.nendo.argosy.data.titledb

expect class RequestSigner {
    class SignedRequest {
        val deviceToken: String
        val timestamp: Long
        val signature: String
        val fpHash: String
    }

    fun sign(deviceToken: String, query: String): SignedRequest?

    fun isConfigured(): Boolean
}
