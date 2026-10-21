package com.nendo.argosy.platform

import androidx.compose.ui.text.platform.FontLoadResult
import org.jetbrains.skia.Font
import org.jetbrains.skia.Typeface
import kotlin.math.ceil
import kotlin.math.floor

actual fun platformTypefaceOf(resolved: Any): Any? = when (resolved) {
    is Typeface -> resolved
    is FontLoadResult -> resolved.typeface
    else -> null
}

actual fun textBoundsHeight(typeface: Any, textSizePx: Float, text: String): Int {
    val bounds = Font(typeface as Typeface, textSizePx).measureText(text)
    return (ceil(bounds.bottom) - floor(bounds.top)).toInt()
}
