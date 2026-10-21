package com.nendo.argosy.platform

import android.provider.OpenableColumns
import java.io.InputStream

actual fun PlatformContentResolver.openInputStream(uri: PlatformUri): InputStream? = openInputStream(uri)

actual fun queryOpenableDisplayName(resolver: PlatformContentResolver, uri: PlatformUri): String? =
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
