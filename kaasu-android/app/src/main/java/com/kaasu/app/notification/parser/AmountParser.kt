package com.kaasu.app.notification.parser

import java.math.BigDecimal
import java.math.RoundingMode

object AmountParser {

    // Matches: ₹1,234.56 | ₹ 1,234 | Rs.1,234 | Rs:500 | Rs 500 | INR 1,234.56 | INR1234
    private val PREFIX_PATTERN = Regex(
        """(?:₹\s*|₨\s*|Rs[.:]?\s*|INR\s*)([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // Matches: 1234.56 INR
    private val SUFFIX_PATTERN = Regex(
        """([\d,]+(?:\.\d{1,2})?)\s*INR""",
        RegexOption.IGNORE_CASE
    )

    // Called by TransactionParser.parse() — null return causes the whole parse to abort (no amount = no transaction)
    fun parse(text: String): Long? {
        val raw = (PREFIX_PATTERN.find(text) ?: SUFFIX_PATTERN.find(text))
            ?.groupValues?.get(1) ?: return null

        val cleaned = raw.replace(",", "")
        val amount = cleaned.toBigDecimalOrNull() ?: return null
        if (amount <= BigDecimal.ZERO) return null

        return amount.multiply(BigDecimal("100"))
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }
}
