package com.nendo.argosy.platform

expect object PlatformHtml {
    val FROM_HTML_MODE_LEGACY: Int

    fun fromHtml(source: String, flags: Int): CharSequence
}

fun String.toColorInt(): Int = PlatformColor.parseColor(this)
