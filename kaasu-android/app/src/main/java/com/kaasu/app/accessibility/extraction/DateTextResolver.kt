package com.kaasu.app.accessibility.extraction

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Resolves the bare, often-relative date fragments GPay/PhonePe show on transaction-history rows
 * ("28 Aug", "Aug 28", "Yesterday", "12/08/2026") into an epoch-millis timestamp. Fully pure/testable
 * — no Android dependency — mirroring how [com.kaasu.app.statement.parser.xlsx.XlsxTextExtractor]'s
 * zip/XML parsing was split into a pure `XlsxParsing` object during the statement-import work.
 *
 * A history row almost never carries a time-of-day, so [resolve] anchors at **noon** on the
 * resolved date when no time fragment is present — a deliberate mid-day anchor (not midnight) so
 * the coarse day-range dedup in `DuplicateChecker.isDuplicateCoarse` and any later same-day
 * comparisons land safely inside the matching calendar day regardless of which side of local
 * midnight the comparison timezone rounds to.
 */
object DateTextResolver {

    private val MONTHS = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
        "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12
    )

    // dd/MM/yyyy or dd-MM-yyyy (also accepts a 2-digit year)
    private val NUMERIC_DATE = Regex("""\b(\d{1,2})[/-](\d{1,2})[/-](\d{2,4})\b""")

    // "28 Aug" / "28 Aug 2026" — day first
    private val DAY_MONTH_YEAR = Regex("""\b(\d{1,2})\s+([A-Za-z]{3,})\s*(\d{4})?\b""")

    // "Aug 28" / "Aug 28, 2026" — month first
    private val MONTH_DAY_YEAR = Regex("""\b([A-Za-z]{3,})\s+(\d{1,2}),?\s*(\d{4})?\b""")

    // "1:45 pm" / "13:45"
    private val TIME_PATTERN = Regex("""\b(\d{1,2}):(\d{2})\s*(am|pm)?\b""", RegexOption.IGNORE_CASE)

    private val TODAY_WORD = Regex("""\btoday\b""", RegexOption.IGNORE_CASE)
    private val YESTERDAY_WORD = Regex("""\byesterday\b""", RegexOption.IGNORE_CASE)

    /** Full resolution: date + time-of-day if present, else the date at noon. Null if unparseable. */
    fun resolve(
        dateText: String?,
        anchorMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Long? {
        val date = resolveDate(dateText, anchorMillis, zone) ?: return null
        val time = resolveTimeOfDay(dateText)
        val dateTime = if (time != null) date.atTime(time) else date.atTime(LocalTime.NOON)
        return dateTime.atZone(zone).toInstant().toEpochMilli()
    }

    /** Just the calendar date, ignoring any time-of-day fragment. Null if unparseable. */
    fun resolveDate(
        dateText: String?,
        anchorMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): LocalDate? {
        if (dateText.isNullOrBlank()) return null
        val text = dateText.trim()
        val today = Instant.ofEpochMilli(anchorMillis).atZone(zone).toLocalDate()

        // Word-boundary match, not exact-equality — a fragment can carry a relative day word
        // alongside a time-of-day, e.g. "Today, 9:05 am" (resolveTimeOfDay picks the time back out
        // of the same original string separately).
        if (TODAY_WORD.containsMatchIn(text)) return today
        if (YESTERDAY_WORD.containsMatchIn(text)) return today.minusDays(1)

        NUMERIC_DATE.find(text)?.let { m ->
            val (d, mo, y) = m.destructured
            val year = if (y.length == 2) 2000 + y.toInt() else y.toInt()
            return runCatching { LocalDate.of(year, mo.toInt(), d.toInt()) }.getOrNull()
        }

        DAY_MONTH_YEAR.find(text)?.let { m ->
            val day = m.groupValues[1].toIntOrNull()
            val month = monthFromAbbrev(m.groupValues[2])
            if (day != null && month != null) {
                val yearGroup = m.groupValues[3]
                val year = if (yearGroup.isNotBlank()) yearGroup.toInt() else inferredYear(today, month, day)
                runCatching { LocalDate.of(year, month, day) }.getOrNull()?.let { return it }
            }
        }

        MONTH_DAY_YEAR.find(text)?.let { m ->
            val month = monthFromAbbrev(m.groupValues[1])
            val day = m.groupValues[2].toIntOrNull()
            if (month != null && day != null) {
                val yearGroup = m.groupValues[3]
                val year = if (yearGroup.isNotBlank()) yearGroup.toInt() else inferredYear(today, month, day)
                runCatching { LocalDate.of(year, month, day) }.getOrNull()?.let { return it }
            }
        }

        return null
    }

    private fun monthFromAbbrev(text: String): Int? = MONTHS[text.take(3).lowercase()]

    // No explicit year on screen ("28 Aug") — assume the current year unless that would place the
    // date more than a week in the future, in which case it's last year's same date scrolled into
    // view (a transaction-history row is never a future date).
    private fun inferredYear(today: LocalDate, month: Int, day: Int): Int {
        val candidate = runCatching { LocalDate.of(today.year, month, day) }.getOrNull() ?: return today.year
        return if (candidate.isAfter(today.plusDays(7))) today.year - 1 else today.year
    }

    private fun resolveTimeOfDay(dateText: String?): LocalTime? {
        if (dateText.isNullOrBlank()) return null
        val m = TIME_PATTERN.find(dateText) ?: return null
        var hour = m.groupValues[1].toIntOrNull() ?: return null
        val minute = m.groupValues[2].toIntOrNull() ?: return null
        when (m.groupValues[3].lowercase()) {
            "pm" -> if (hour != 12) hour += 12
            "am" -> if (hour == 12) hour = 0
        }
        return runCatching { LocalTime.of(hour, minute) }.getOrNull()
    }
}
