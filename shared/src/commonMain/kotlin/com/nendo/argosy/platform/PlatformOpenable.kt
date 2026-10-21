package com.nendo.argosy.platform

import java.io.InputStream

expect fun PlatformContentResolver.openInputStream(uri: PlatformUri): InputStream?

/**
 * The OpenableColumns.DISPLAY_NAME of [uri], or null when the resolver has no row for it (on
 * Android, any uri that is not a content uri).
 */
expect fun queryOpenableDisplayName(resolver: PlatformContentResolver, uri: PlatformUri): String?
