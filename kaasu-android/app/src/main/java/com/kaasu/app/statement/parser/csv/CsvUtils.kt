package com.kaasu.app.statement.parser.csv

import java.math.BigDecimal
import java.math.RoundingMode

// Shared helpers for the per-bank CSV parsers — kept internal (module-visible) rather than
// duplicated in each parser file.

/**
 * Minimal RFC-4180-ish CSV line splitter: honors double-quoted fields (so a quoted narration
 * containing a comma isn't split into extra columns) and unescapes "" to a literal quote.
 * Does not handle embedded newlines inside a quoted field — bank statement narrations are
 * single-line in every format this app supports, so that's an acceptable first-pass limitation.
 */
internal fun splitCsvLine(line: String): List<String> {
    val fields = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            c == '"' -> {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    current.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            }
            c == ',' && !inQuotes -> {
                fields.add(current.toString())
                current.clear()
            }
            else -> current.append(c)
        }
        i++
    }
    fields.add(current.toString())
    return fields.map { it.trim() }
}

/**
 * Parses a statement amount cell ("1,234.50", "₹1,234.50", blank, "-") to paise. Returns null
 * for a blank/non-numeric cell — callers use that to detect "not applicable to this row"
 * (e.g. an empty Withdrawal Amt. cell on a credit row) rather than treating it as an error.
 */
internal fun parseAmountToPaise(raw: String): Long? {
    val cleaned = raw.trim()
        .removePrefix("₹").removePrefix("Rs.").removePrefix("Rs")
        .replace(",", "")
        .trim()
    if (cleaned.isBlank() || cleaned == "-") return null
    val amount = cleaned.toBigDecimalOrNull() ?: return null
    if (amount.signum() <= 0) return null
    return amount.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).toLong()
}
