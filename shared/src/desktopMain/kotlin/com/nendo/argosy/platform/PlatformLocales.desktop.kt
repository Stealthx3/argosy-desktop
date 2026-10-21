package com.nendo.argosy.platform

import java.util.Locale

private val systemLocale: Locale = Locale.getDefault()

actual fun setApplicationLocales(context: PlatformContext, tag: String?) {
    Locale.setDefault(if (tag == null) systemLocale else Locale.forLanguageTag(tag))
}
