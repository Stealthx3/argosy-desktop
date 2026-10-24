package com.nendo.argosy.platform

import com.nendo.argosy.data.model.SandboxPermission
import com.nendo.argosy.data.model.SandboxPermissionReport

/**
 * Reads the permissions the Flatpak sandbox grants this process. [isSupported] is false where
 * Argosy is not a Flatpak app. [grant] adds a user override on the host and returns whether the
 * host now lists [permission] as granted; the sandbox sees it from the next launch.
 */
expect class SandboxPermissions {
    val isSupported: Boolean

    suspend fun read(): SandboxPermissionReport

    suspend fun grant(permission: SandboxPermission, flags: List<String>): Boolean
}
