package com.nendo.argosy.platform

import java.io.File

expect abstract class PlatformUri {
    abstract override fun toString(): String
}

expect val PlatformUri.scheme: String?

expect val PlatformUri.authority: String?

expect val PlatformUri.host: String?

expect val PlatformUri.path: String?

expect val PlatformUri.encodedPath: String?

expect val PlatformUri.lastPathSegment: String?

expect val PlatformUri.pathSegments: List<String>

expect val PlatformUri.queryParameterNames: Set<String>

expect fun PlatformUri.getQueryParameter(key: String): String?

expect object PlatformUris {
    fun parse(uriString: String): PlatformUri
    fun fromFile(file: File): PlatformUri
    fun encode(s: String?): String?
    fun decode(s: String?): String?
}
