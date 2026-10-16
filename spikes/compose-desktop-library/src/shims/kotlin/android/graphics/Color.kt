package android.graphics

/** The android.graphics.Color statics upstream's theme uses (HSV in Android's 0..360, 0..1, 0..1 ranges). */
object Color {
    fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int =
        (alpha shl 24) or (red shl 16) or (green shl 8) or blue

    fun colorToHSV(color: Int, hsv: FloatArray) {
        val hsb = java.awt.Color.RGBtoHSB((color shr 16) and 0xFF, (color shr 8) and 0xFF, color and 0xFF, null)
        hsv[0] = hsb[0] * 360f; hsv[1] = hsb[1]; hsv[2] = hsb[2]
    }

    fun HSVToColor(hsv: FloatArray): Int = HSVToColor(0xFF, hsv)

    fun HSVToColor(alpha: Int, hsv: FloatArray): Int {
        val rgb = java.awt.Color.HSBtoRGB((hsv[0] / 360f).mod(1f), hsv[1].coerceIn(0f, 1f), hsv[2].coerceIn(0f, 1f))
        return (alpha shl 24) or (rgb and 0xFFFFFF)
    }
}
