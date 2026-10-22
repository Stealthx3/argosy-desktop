package com.nendo.argosy.platform

import com.nendo.argosy.util.Logger
import java.awt.Window

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

/**
 * Compose's window content component answers `getInputMethodRequests()` only while a text field
 * holds an input session, which is what Android's `isAcceptingText` reports.
 */
actual val PlatformInputMethodManager.isAcceptingText: Boolean get() = isDesktopTextInputActive()

fun isDesktopTextInputActive(): Boolean =
    Window.getWindows().any { it.isShowing && it.mostRecentFocusOwner?.inputMethodRequests != null }

actual object PlatformInputMethod {
    actual val SHOW_FORCED: Int = 2

    private val desktopManager = PlatformInputMethodManager()

    actual fun manager(context: PlatformContext): PlatformInputMethodManager? = desktopManager
}
