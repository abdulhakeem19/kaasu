package com.kaasu.app.accessibility.extraction

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DateTextResolverTest {

    private val zone = ZoneId.of("Asia/Kolkata")

    // Anchor "now" at 2026-08-30 (a Sunday), matching the current-session date this feature was
    // built against — every relative/inferred-year assertion below is relative to this anchor.
    private val anchor = ZonedDateTime.of(2026, 8, 30, 18, 0, 0, 0, zone).toInstant().toEpochMilli()

    @Test fun resolvesToday() {
        val date = DateTextResolver.resolveDate("Today", anchor, zone)
        assertEquals(LocalDate.of(2026, 8, 30), date)
    }

    @Test fun resolvesYesterday() {
        val date = DateTextResolver.resolveDate("Yesterday", anchor, zone)
        assertEquals(LocalDate.of(2026, 8, 29), date)
    }

    @Test fun resolvesDayMonth_noYear_inCurrentYear() {
        val date = DateTextResolver.resolveDate("28 Aug", anchor, zone)
        assertEquals(LocalDate.of(2026, 8, 28), date)
    }

    @Test fun resolvesMonthDay_noYear_inCurrentYear() {
        val date = DateTextResolver.resolveDate("Aug 28", anchor, zone)
        assertEquals(LocalDate.of(2026, 8, 28), date)
    }

    @Test fun resolvesMonthDayWithComma() {
        val date = DateTextResolver.resolveDate("Aug 28, 2026", anchor, zone)
        assertEquals(LocalDate.of(2026, 8, 28), date)
    }

    @Test fun resolvesDayMonthYear_explicitYear() {
        val date = DateTextResolver.resolveDate("15 Jan 2026", anchor, zone)
        assertEquals(LocalDate.of(2026, 1, 15), date)
    }

    @Test fun resolvesSlashDate_ddMMyyyy() {
        val date = DateTextResolver.resolveDate("28/08/2026", anchor, zone)
        assertEquals(LocalDate.of(2026, 8, 28), date)
    }

    @Test fun resolvesDashDate_twoDigitYear() {
        val date = DateTextResolver.resolveDate("28-08-26", anchor, zone)
        assertEquals(LocalDate.of(2026, 8, 28), date)
    }

    // A bare "d MMM" more than a week ahead of "today" must be last year's date scrolled into
    // view — a transaction-history row is never a future date.
    @Test fun inferredYear_rollsBackWhenDateWouldBeInTheFuture() {
        val date = DateTextResolver.resolveDate("15 Dec", anchor, zone)
        assertEquals(LocalDate.of(2025, 12, 15), date)
    }

    @Test fun inferredYear_keepsCurrentYearWhenWithinAWeekAhead() {
        // anchor is 30 Aug 2026; 2 Sep is within the 7-day forward tolerance.
        val date = DateTextResolver.resolveDate("2 Sep", anchor, zone)
        assertEquals(LocalDate.of(2026, 9, 2), date)
    }

    @Test fun returnsNullForBlankOrUnparseable() {
        assertNull(DateTextResolver.resolveDate(null, anchor, zone))
        assertNull(DateTextResolver.resolveDate("", anchor, zone))
        assertNull(DateTextResolver.resolveDate("not a date", anchor, zone))
    }

    @Test fun resolve_anchorsAtNoonWhenNoTimeFragmentPresent() {
        val millis = DateTextResolver.resolve("28 Aug", anchor, zone)!!
        val dateTime = Instant.ofEpochMilli(millis).atZone(zone)
        assertEquals(LocalDate.of(2026, 8, 28), dateTime.toLocalDate())
        assertEquals(LocalTime.NOON, dateTime.toLocalTime())
    }

    @Test fun resolve_usesExplicitTimeFragmentWhenPresent() {
        val millis = DateTextResolver.resolve("28 Aug 2026, 1:45 pm", anchor, zone)!!
        val dateTime = Instant.ofEpochMilli(millis).atZone(zone)
        assertEquals(LocalDate.of(2026, 8, 28), dateTime.toLocalDate())
        assertEquals(LocalTime.of(13, 45), dateTime.toLocalTime())
    }

    @Test fun resolve_handlesAmTimeFragment() {
        val millis = DateTextResolver.resolve("Today, 9:05 am", anchor, zone)!!
        val dateTime = Instant.ofEpochMilli(millis).atZone(zone)
        assertEquals(LocalTime.of(9, 5), dateTime.toLocalTime())
    }

    @Test fun resolve_returnsNullWhenDateUnparseable() {
        assertNull(DateTextResolver.resolve("garbled text", anchor, zone))
    }
}
