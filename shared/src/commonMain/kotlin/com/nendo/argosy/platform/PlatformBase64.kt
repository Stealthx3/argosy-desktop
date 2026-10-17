package com.nendo.argosy.platform

expect object PlatformBase64 {
    val DEFAULT: Int
    val NO_PADDING: Int
    val NO_WRAP: Int
    val CRLF: Int
    val URL_SAFE: Int

    fun decode(str: String, flags: Int): ByteArray
    fun decode(input: ByteArray, flags: Int): ByteArray
    fun encode(input: ByteArray, flags: Int): ByteArray
    fun encodeToString(input: ByteArray, flags: Int): String
}
