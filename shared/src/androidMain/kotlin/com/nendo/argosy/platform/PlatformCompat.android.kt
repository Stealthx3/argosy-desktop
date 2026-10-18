package com.nendo.argosy.platform

import android.content.ActivityNotFoundException
import android.view.Display
import androidx.core.content.ContextCompat

actual typealias PlatformActivityNotFoundException = ActivityNotFoundException

actual typealias PlatformDisplay = Display

actual val PlatformDisplay.displayId: Int get() = displayId

actual object PlatformContextCompat {
    actual fun getDisplayOrDefault(context: PlatformContext): PlatformDisplay = ContextCompat.getDisplayOrDefault(context)

    actual fun checkSelfPermission(context: PlatformContext, permission: String): Int =
        ContextCompat.checkSelfPermission(context, permission)
}
