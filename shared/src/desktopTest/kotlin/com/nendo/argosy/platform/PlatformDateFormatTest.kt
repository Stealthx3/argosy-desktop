package com.nendo.argosy.platform

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformDateFormatTest {
    private val date = LocalDate.of(2026, 10, 17)

    private fun format(locale: Locale, skeleton: String): String =
        DateTimeFormatter.ofPattern(PlatformDateFormat.getBestDateTimePattern(locale, skeleton), locale).format(date)

    @Test
    fun monthDayFollowsTheLocaleFieldOrder() {
        assertEquals("Oct 17", format(Locale.US, "MMMd"))
        assertEquals("17 Oct", format(Locale.UK, "MMMd"))
    }

    @Test
    fun skeletonFieldsMayComeInAnyOrder() {
        assertEquals("October 2026", format(Locale.US, "MMMMyyyy"))
        assertEquals("Oct 17, 2026", format(Locale.US, "MMMdyyyy"))
    }
}
