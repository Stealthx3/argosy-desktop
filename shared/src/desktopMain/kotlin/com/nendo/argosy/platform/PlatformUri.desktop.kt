package com.nendo.argosy.platform

import java.io.File
import java.net.URI
import java.net.URLDecoder

actual abstract class PlatformUri {
    internal abstract val raw: String

    actual abstract override fun toString(): String
}

private class DesktopUri(override val raw: String) : PlatformUri() {
    val parsed: URI? = runCatching { URI(raw) }.getOrNull()

    override fun toString(): String = raw

    override fun equals(other: Any?): Boolean = other is PlatformUri && other.raw == raw

    override fun hashCode(): Int = raw.hashCode()
}

private val PlatformUri.parsed: URI? get() = (this as DesktopUri).parsed

actual val PlatformUri.scheme: String? get() = parsed?.scheme ?: raw.substringBefore(':', "").ifEmpty { null }

actual val PlatformUri.authority: String? get() = parsed?.authority

actual val PlatformUri.host: String? get() = parsed?.host

actual val PlatformUri.path: String? get() = parsed?.path

actual val PlatformUri.encodedPath: String? get() = parsed?.rawPath

actual val PlatformUri.lastPathSegment: String? get() = pathSegments.lastOrNull()

actual val PlatformUri.pathSegments: List<String> get() = path.orEmpty().split('/').filter { it.isNotEmpty() }

private val PlatformUri.queryPairs: List<Pair<String, String>>
    get() = parsed?.rawQuery.orEmpty().split('&').filter { it.isNotEmpty() }.map { part ->
        val key = part.substringBefore('=')
        val value = if ('=' in part) part.substringAfter('=') else ""
        PlatformUris.decode(key).orEmpty() to PlatformUris.decode(value).orEmpty()
    }

actual val PlatformUri.queryParameterNames: Set<String> get() = queryPairs.mapTo(LinkedHashSet()) { it.first }

actual fun PlatformUri.getQueryParameter(key: String): String? = queryPairs.firstOrNull { it.first == key }?.second

actual object PlatformUris {
    actual fun parse(uriString: String): PlatformUri = DesktopUri(uriString)
    actual fun fromFile(file: File): PlatformUri = DesktopUri(file.toURI().toString().replaceFirst("file:/", "file:///").replace("file:////", "file:///"))
    actual fun encode(s: String?): String? = s?.let { encode(it) }
    actual fun decode(s: String?): String? = s?.let { decode(it) }
    @JvmName("encodeNonNull")
    actual fun encode(s: String): String = buildString {
        for (byte in s.toByteArray(Charsets.UTF_8)) {
            val c = byte.toInt().toChar()
            if (byte >= 0 && (c.isLetterOrDigit() || c in UNRESERVED)) append(c)
            else append('%').append(HEX[(byte.toInt() shr 4) and 0xF]).append(HEX[byte.toInt() and 0xF])
        }
    }
    @JvmName("decodeNonNull")
    actual fun decode(s: String): String = URLDecoder.decode(s.replace("+", "%2B"), Charsets.UTF_8)

    private const val UNRESERVED = "_-!.~'()*"
    private const val HEX = "0123456789ABCDEF"
}
