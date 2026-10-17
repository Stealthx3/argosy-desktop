package com.nendo.argosy.ui.screens.settings.sections

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.Locale

internal fun formatCoreBuildStamp(raw: String, zone: ZoneId, locale: Locale): String =
    try {
        ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME)
            .withZoneSameInstant(zone)
            .format(
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                    .withLocale(locale)
            )
    } catch (_: DateTimeParseException) {
        raw
    }
