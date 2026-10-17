package com.nendo.argosy.hardware

import com.nendo.argosy.platform.PlatformIntentType as Intent
import com.nendo.argosy.platform.action
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.data
import com.nendo.argosy.platform.flags
import com.nendo.argosy.platform.putExtra
import com.nendo.argosy.platform.component
import com.nendo.argosy.platform.`package`
import com.nendo.argosy.platform.getStringExtra
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.PlatformIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

expect class ScreenCaptureManager {
    companion object {}

    val isCapturing: StateFlow<Boolean>

    val hasPermission: StateFlow<Boolean>

    fun onPermissionResult(resultCode: Int, data: Intent?)

    fun startCapture()

    fun stopCapture()

    fun clearPermission()
}
