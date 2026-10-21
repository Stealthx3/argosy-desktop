package com.nendo.argosy.platform

/**
 * The platform typeface behind a value from FontFamily.Resolver.resolve, or null when it holds
 * none (Android: an android.graphics.Typeface).
 */
expect fun platformTypefaceOf(resolved: Any): Any?

/**
 * Height in whole pixels of the ink bounds of [text] drawn in [typeface] at [textSizePx], as
 * Paint.getTextBounds reports it. [typeface] is a value returned by [platformTypefaceOf].
 */
expect fun textBoundsHeight(typeface: Any, textSizePx: Float, text: String): Int
