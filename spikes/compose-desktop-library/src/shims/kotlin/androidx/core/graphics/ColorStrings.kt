package androidx.core.graphics

/** androidx.core String.toColorInt(): #RRGGBB or #AARRGGBB. */
fun String.toColorInt(): Int {
    val hex = removePrefix("#")
    val v = hex.toLong(16)
    return when (hex.length) {
        6 -> (0xFF000000 or v).toInt()
        8 -> v.toInt()
        else -> throw IllegalArgumentException("Unknown color: $this")
    }
}
