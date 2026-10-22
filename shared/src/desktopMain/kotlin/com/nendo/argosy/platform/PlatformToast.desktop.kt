package com.nendo.argosy.platform

import com.nendo.argosy.util.Logger

actual class PlatformToastType(internal val text: String)

actual fun PlatformToastType.show() {
    Logger.info("Toast", text)
}

actual object PlatformToasts {
    actual val LENGTH_SHORT: Int = 0
    actual val LENGTH_LONG: Int = 1

    actual fun makeText(context: PlatformContext, resId: Int, duration: Int): PlatformToastType =
        PlatformToastType(context.getString(resId))

    actual fun makeText(context: PlatformContext, text: CharSequence, duration: Int): PlatformToastType =
        PlatformToastType(text.toString())
}

actual class PlatformInputMethodManager

actual fun PlatformInputMethodManager.toggleSoftInput(showFlags: Int, hideFlags: Int) {
}

actual val PlatformInputMethodManager.isAcceptingText: Boolean get() = false

actual object PlatformInputMethod {
    actual val SHOW_FORCED: Int = 2

    actual fun manager(context: PlatformContext): PlatformInputMethodManager? = null
}
