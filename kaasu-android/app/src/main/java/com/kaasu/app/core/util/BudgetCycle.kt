package com.kaasu.app.core.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Computes the current budget cycle window for a given month-start day (1–28). With startDay = 1
 * this is just the calendar month. With startDay = 15, the cycle runs 15th → 14th of next month.
 */
data class BudgetCycle(val startMillis: Long, val endMillis: Long, val label: String) {
    companion object {
        fun current(startDay: Int, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): BudgetCycle {
            val day = startDay.coerceIn(1, 28)
            // Cycle start: the most recent occurrence of `day` on/before today.
            val start = if (today.dayOfMonth >= day) today.withDayOfMonth(day)
            else today.minusMonths(1).withDayOfMonth(day)
            val end = start.plusMonths(1).minusDays(1)

            val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = end.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

            // Label: "June" for calendar months, otherwise "14 Jun – 13 Jul".
            val label = if (day == 1) {
                start.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " ${start.year}"
            } else {
                val f = java.time.format.DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
                "${start.format(f)} – ${end.format(f)}"
            }
            return BudgetCycle(startMillis, endMillis, label)
        }
    }
}
