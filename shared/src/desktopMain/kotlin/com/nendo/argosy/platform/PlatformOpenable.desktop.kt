package com.nendo.argosy.platform

import java.io.File
import java.io.InputStream

actual fun PlatformContentResolver.openInputStream(uri: PlatformUri): InputStream? {
    if (uri.scheme != null && uri.scheme != "file") return null
    val file = uri.path?.let(::File) ?: return null
    return if (file.isFile) file.inputStream() else null
}

actual fun PlatformContentResolver.takePersistableUriPermission(uri: PlatformUri, modeFlags: Int) {}

actual fun queryOpenableDisplayName(resolver: PlatformContentResolver, uri: PlatformUri): String? = null
