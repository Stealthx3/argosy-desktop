package com.nendo.argosy.platform

import com.nendo.argosy.data.model.SandboxPermissionReport
import javax.inject.Inject

actual class SandboxPermissions @Inject constructor() {
    actual val isSupported: Boolean = false

    actual suspend fun read(): SandboxPermissionReport = SandboxPermissionReport()
}
