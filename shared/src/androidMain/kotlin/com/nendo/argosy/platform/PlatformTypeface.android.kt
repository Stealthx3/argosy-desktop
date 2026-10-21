package com.nendo.argosy.platform

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface

actual fun platformTypefaceOf(resolved: Any): Any? = resolved as? Typeface

actual fun textBoundsHeight(typeface: Any, textSizePx: Float, text: String): Int {
    val bounds = Rect()
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface as Typeface
        textSize = textSizePx
    }.getTextBounds(text, 0, text.length, bounds)
    return bounds.height()
}
