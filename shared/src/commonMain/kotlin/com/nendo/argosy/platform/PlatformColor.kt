package com.nendo.argosy.platform

expect object PlatformColor {
    val BLACK: Int
    val WHITE: Int
    val TRANSPARENT: Int
    fun alpha(color: Int): Int
    fun red(color: Int): Int
    fun green(color: Int): Int
    fun blue(color: Int): Int
    fun rgb(red: Int, green: Int, blue: Int): Int
    fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int
    fun parseColor(colorString: String): Int
    fun colorToHSV(color: Int, hsv: FloatArray)
    fun HSVToColor(hsv: FloatArray): Int
    fun HSVToColor(alpha: Int, hsv: FloatArray): Int
}
