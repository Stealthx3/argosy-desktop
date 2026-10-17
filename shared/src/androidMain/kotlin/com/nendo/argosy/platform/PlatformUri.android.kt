package com.nendo.argosy.platform

import android.net.Uri
import java.io.File

actual typealias PlatformUri = Uri

actual val PlatformUri.scheme: String? get() = scheme

actual val PlatformUri.authority: String? get() = authority

actual val PlatformUri.host: String? get() = host

actual val PlatformUri.path: String? get() = path

actual val PlatformUri.encodedPath: String? get() = encodedPath

actual val PlatformUri.lastPathSegment: String? get() = lastPathSegment

actual val PlatformUri.pathSegments: List<String> get() = pathSegments

actual val PlatformUri.queryParameterNames: Set<String> get() = queryParameterNames

actual fun PlatformUri.getQueryParameter(key: String): String? = getQueryParameter(key)

actual object PlatformUris {
    actual fun parse(uriString: String): PlatformUri = Uri.parse(uriString)
    actual fun fromFile(file: File): PlatformUri = Uri.fromFile(file)
    actual fun encode(s: String?): String? = Uri.encode(s)
    actual fun decode(s: String?): String? = Uri.decode(s)
    actual fun encode(s: String): String = Uri.encode(s)
    actual fun decode(s: String): String = Uri.decode(s)
}
