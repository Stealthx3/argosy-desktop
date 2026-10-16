package android.text.format

import android.content.Context
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.time.chrono.IsoChronology
import java.util.Locale

object DateFormat {
    /** Best-effort: Android's ICU skeleton matcher has no JDK equivalent, so use the locale's medium date. */
    fun getBestDateTimePattern(locale: Locale, skeleton: String): String =
        DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.MEDIUM, null, IsoChronology.INSTANCE, locale)

    fun getTimeFormat(context: Context): java.text.DateFormat =
        SimpleDateFormat(if (is24HourFormat(context)) "HH:mm" else "h:mm a", Locale.getDefault())

    fun is24HourFormat(context: Context): Boolean = true
}
