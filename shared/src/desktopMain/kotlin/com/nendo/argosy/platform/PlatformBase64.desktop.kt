package com.nendo.argosy.platform

import java.util.Base64

actual object PlatformBase64 {
    actual val DEFAULT: Int = 0
    actual val NO_PADDING: Int = 1
    actual val NO_WRAP: Int = 2
    actual val CRLF: Int = 4
    actual val URL_SAFE: Int = 8

    private const val LINE_LENGTH = 76

    actual fun decode(str: String, flags: Int): ByteArray {
        val compact = str.filterNot { it.isWhitespace() }
        val decoder = if (flags and URL_SAFE != 0) Base64.getUrlDecoder() else Base64.getDecoder()
        return decoder.decode(compact)
    }

    actual fun decode(input: ByteArray, flags: Int): ByteArray = decode(String(input, Charsets.US_ASCII), flags)

    actual fun encode(input: ByteArray, flags: Int): ByteArray = encodeToString(input, flags).toByteArray(Charsets.US_ASCII)

    actual fun encodeToString(input: ByteArray, flags: Int): String {
        val base = if (flags and URL_SAFE != 0) Base64.getUrlEncoder() else Base64.getEncoder()
        val encoder = if (flags and NO_PADDING != 0) base.withoutPadding() else base
        val encoded = encoder.encodeToString(input)
        if (flags and NO_WRAP != 0 || encoded.isEmpty()) return encoded
        val separator = if (flags and CRLF != 0) "\r\n" else "\n"
        return encoded.chunked(LINE_LENGTH).joinToString(separator, postfix = separator)
    }
}
