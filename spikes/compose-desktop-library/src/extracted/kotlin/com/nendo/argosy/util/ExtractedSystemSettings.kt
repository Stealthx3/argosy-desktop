// Extracted verbatim from app/src/main/kotlin/com/nendo/argosy/util/SystemSettings.kt: formatClockTime
package com.nendo.argosy.util

import android.content.Context
import android.text.format.DateFormat
import java.util.Date

/**
 * Formats a wall-clock time in the 12- or 24-hour form the device is configured for.
 */
fun formatClockTime(context: Context, epochMillis: Long): String =
    DateFormat.getTimeFormat(context).format(Date(epochMillis))
