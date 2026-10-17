package com.nendo.argosy.platform

import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

actual object PlatformDateFormat {
    private const val FIELD_ORDER = "GyQMwEdBhHjCmsvz"

    actual fun getBestDateTimePattern(locale: Locale, skeleton: String): String {
        val template = skeleton.toList()
            .sortedBy { FIELD_ORDER.indexOf(it).takeIf { index -> index >= 0 } ?: FIELD_ORDER.length }
            .joinToString("")
        return DateTimeFormatterBuilder.getLocalizedDateTimePattern(template, IsoChronology.INSTANCE, locale)
    }
}
