package com.nendo.argosy.platform

import android.text.format.DateFormat
import java.util.Locale

actual object PlatformDateFormat {
    actual fun getBestDateTimePattern(locale: Locale, skeleton: String): String =
        DateFormat.getBestDateTimePattern(locale, skeleton)
}
