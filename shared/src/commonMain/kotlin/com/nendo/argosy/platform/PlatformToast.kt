package com.nendo.argosy.platform

expect class PlatformToastType

expect fun PlatformToastType.show()

expect object PlatformToasts {
    val LENGTH_SHORT: Int
    val LENGTH_LONG: Int

    fun makeText(context: PlatformContext, resId: Int, duration: Int): PlatformToastType

    fun makeText(context: PlatformContext, text: CharSequence, duration: Int): PlatformToastType
}

expect class PlatformInputMethodManager

expect fun PlatformInputMethodManager.toggleSoftInput(showFlags: Int, hideFlags: Int)

expect object PlatformInputMethod {
    val SHOW_FORCED: Int

    fun manager(context: PlatformContext): PlatformInputMethodManager?
}
