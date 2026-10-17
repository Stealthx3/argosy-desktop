package com.nendo.argosy.platform

import android.graphics.Color

actual object PlatformColor {
    actual val BLACK: Int get() = Color.BLACK
    actual val WHITE: Int get() = Color.WHITE
    actual val TRANSPARENT: Int get() = Color.TRANSPARENT
    actual fun alpha(color: Int): Int = Color.alpha(color)
    actual fun red(color: Int): Int = Color.red(color)
    actual fun green(color: Int): Int = Color.green(color)
    actual fun blue(color: Int): Int = Color.blue(color)
    actual fun rgb(red: Int, green: Int, blue: Int): Int = Color.rgb(red, green, blue)
    actual fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int = Color.argb(alpha, red, green, blue)
    actual fun parseColor(colorString: String): Int = Color.parseColor(colorString)
    actual fun colorToHSV(color: Int, hsv: FloatArray) = Color.colorToHSV(color, hsv)
    actual fun HSVToColor(hsv: FloatArray): Int = Color.HSVToColor(hsv)
    actual fun HSVToColor(alpha: Int, hsv: FloatArray): Int = Color.HSVToColor(alpha, hsv)
}
