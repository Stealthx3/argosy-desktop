package com.nendo.argosy.util

import com.nendo.argosy.platform.PlatformContext as Context
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val CLOCK_DATE_PATTERN = "MMM d"

actual fun formatClockTime(context: Context, epochMillis: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault(Locale.Category.FORMAT)).format(Date(epochMillis))

actual fun formatClockDateTime(context: Context, epochMillis: Long): String {
    val date = SimpleDateFormat(CLOCK_DATE_PATTERN, Locale.getDefault()).format(Date(epochMillis))
    return "$date, ${formatClockTime(context, epochMillis)}"
}
