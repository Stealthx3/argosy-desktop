package com.nendo.argosy.platform

import android.util.Base64

actual object PlatformBase64 {
    actual val DEFAULT: Int get() = Base64.DEFAULT
    actual val NO_PADDING: Int get() = Base64.NO_PADDING
    actual val NO_WRAP: Int get() = Base64.NO_WRAP
    actual val CRLF: Int get() = Base64.CRLF
    actual val URL_SAFE: Int get() = Base64.URL_SAFE

    actual fun decode(str: String, flags: Int): ByteArray = Base64.decode(str, flags)
    actual fun decode(input: ByteArray, flags: Int): ByteArray = Base64.decode(input, flags)
    actual fun encode(input: ByteArray, flags: Int): ByteArray = Base64.encode(input, flags)
    actual fun encodeToString(input: ByteArray, flags: Int): String = Base64.encodeToString(input, flags)
}
