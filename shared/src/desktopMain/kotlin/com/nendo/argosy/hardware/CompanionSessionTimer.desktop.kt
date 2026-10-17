package com.nendo.argosy.hardware

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import javax.inject.Inject

actual class CompanionSessionTimer @Inject constructor() {
    actual fun start(context: Context) {
    }

    actual fun stop(context: Context) {
    }

    actual fun getActiveMillis(): Long = 0L
}
