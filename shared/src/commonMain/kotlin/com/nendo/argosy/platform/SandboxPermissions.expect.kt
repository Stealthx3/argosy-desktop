package com.nendo.argosy.platform

import com.nendo.argosy.data.model.SandboxPermissionReport

/**
 * Reads the permissions the Flatpak sandbox grants this process. [isSupported] is false where
 * Argosy is not a Flatpak app.
 */
expect class SandboxPermissions {
    val isSupported: Boolean

    suspend fun read(): SandboxPermissionReport
}
