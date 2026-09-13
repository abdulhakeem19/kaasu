package com.kaasu.app.core.util

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * The currency symbol used by [formatRupees] across the app. Held at process level (rather than
 * threaded through every composable) and refreshed from DataStore at app start / when changed.
 * Defaults to ₹.
 */
object CurrencySymbol {
    @Volatile var value: String = "₹"
}

fun Long.formatRupees(): String {
    val rupees = this / 100
    val paise = this % 100
    val rupeesFormatted = "%,d".format(rupees)
    val symbol = CurrencySymbol.value
    return if (paise == 0L) "$symbol$rupeesFormatted"
    else "$symbol$rupeesFormatted.${paise.toString().padStart(2, '0')}"
}

fun String.parseToPaise(): Long? {
    val cleaned = trim().replace(",", "")
    val bd = cleaned.toBigDecimalOrNull() ?: return null
    if (bd <= BigDecimal.ZERO) return null
    return bd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).toLong()
}

fun Long.toAmountDisplay(): String {
    val rupees = this / 100
    val paise = this % 100
    return if (paise == 0L) rupees.toString()
    else "$rupees.${paise.toString().padStart(2, '0')}"
}
