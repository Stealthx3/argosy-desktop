package com.nendo.argosy.hardware

import com.nendo.argosy.platform.PlatformIntentType as Intent
import com.nendo.argosy.platform.PlatformIntent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
actual class ScreenCaptureManager @Inject constructor() {
    actual companion object {}

    actual val isCapturing: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual val hasPermission: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual fun onPermissionResult(resultCode: Int, data: Intent?) {
    }

    actual fun startCapture() {
    }

    actual fun stopCapture() {
    }

    actual fun clearPermission() {
    }
}
