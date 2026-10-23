package com.nendo.argosy.data.remote.playstore

/**
 * Whether app ids on this platform are Play Store package names. On Android they are; on the
 * desktop they are desktop-entry ids, which the store does not know, so [PlayStoreService] skips
 * the request and callers fall back to the app's own icon.
 */
expect object PlayStoreAccess {
    val available: Boolean
}
