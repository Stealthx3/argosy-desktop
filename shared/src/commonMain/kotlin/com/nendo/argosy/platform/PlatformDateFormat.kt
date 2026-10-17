package com.nendo.argosy.platform

import java.util.Locale

expect object PlatformDateFormat {
    fun getBestDateTimePattern(locale: Locale, skeleton: String): String
}
