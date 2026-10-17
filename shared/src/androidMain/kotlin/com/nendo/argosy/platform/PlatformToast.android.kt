package com.nendo.argosy.platform

import android.view.inputmethod.InputMethodManager
import android.widget.Toast

actual typealias PlatformToastType = Toast

actual fun PlatformToastType.show() = show()

actual object PlatformToasts {
    actual val LENGTH_SHORT: Int = Toast.LENGTH_SHORT
    actual val LENGTH_LONG: Int = Toast.LENGTH_LONG

    actual fun makeText(context: PlatformContext, resId: Int, duration: Int): PlatformToastType =
        Toast.makeText(context, resId, duration)

    actual fun makeText(context: PlatformContext, text: CharSequence, duration: Int): PlatformToastType =
        Toast.makeText(context, text, duration)
}

actual typealias PlatformInputMethodManager = InputMethodManager

@Suppress("DEPRECATION")
actual fun PlatformInputMethodManager.toggleSoftInput(showFlags: Int, hideFlags: Int) = toggleSoftInput(showFlags, hideFlags)

actual object PlatformInputMethod {
    actual val SHOW_FORCED: Int = InputMethodManager.SHOW_FORCED

    actual fun manager(context: PlatformContext): PlatformInputMethodManager? =
        context.getSystemService(InputMethodManager::class.java)
}
