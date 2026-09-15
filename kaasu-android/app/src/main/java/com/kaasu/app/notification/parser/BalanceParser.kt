package com.kaasu.app.notification.parser

/**
 * Pulls the balance the bank states in its own message.
 *
 * Indian banks volunteer this constantly — "Avl Bal: Rs.11,980.50", "Bal INR 1,234" — and the
 * codebase already recognises the phrase in two places without ever reading the number
 * ([com.kaasu.app.notification.filter.PromotionalDetector] treats it as proof of a real
 * transaction, `NoteParser` strips it as boilerplate).
 *
 * That makes it free ground truth for reconciliation: no new permission, no network, nothing the
 * owner has to do. It is never used *as* the balance — only shown beside the derived figure so a
 * disagreement is visible, because silently adopting it would hide every message Kaasu missed.
 */
object BalanceParser {

    private const val AMOUNT = """([\d,]+(?:\.\d{1,2})?)"""

    /**
     * Ordered by how unambiguous the wording is.
     *
     * "Avl Bal" can only mean the balance. A bare "Bal" is likelier to be a stray word, so it comes
     * last and demands a currency marker.
     */
    private val PATTERNS = listOf(
        // The figure sits directly after the phrase: "Avl Bal: Rs.11,980.50", "Closing balance: 99.50".
        Regex("""(?:avl|avbl|available|a\/c|closing|total)\s*(?:bal|balance)\s*(?:is)?\s*[:\-]?\s*(?:₹|₨|Rs[.:]?|INR)?\s*$AMOUNT""", RegexOption.IGNORE_CASE),
        // Banks routinely put the account in between: "Avl Bal in A/c XX5678 is Rs.11,980.50".
        // A currency marker is required here, since without the adjacency there is nothing else
        // distinguishing the balance from the account number that precedes it.
        Regex("""(?:avl|avbl|available|closing|total)\s*(?:bal|balance)\b.{0,40}?(?:₹|₨|Rs[.:]?|INR)\s*$AMOUNT""", RegexOption.IGNORE_CASE),
        // A bare "bal" is weak enough that it demands a currency marker right there.
        Regex("""(?:bal|balance)\s*(?:is)?\s*[:\-]?\s*(?:₹|₨|Rs[.:]?|INR)\s*$AMOUNT""", RegexOption.IGNORE_CASE),
    )

    /**
     * A balance-only message: the bank stating a figure with no money having moved.
     *
     * These are the most reliable balance statements there are, and the pipeline throws them away
     * today — [com.kaasu.app.notification.filter.PromotionalDetector] keeps them (they carry a
     * transactional signal) but the parser then finds no transaction and drops the whole message.
     */
    private val MOVEMENT_WORDS = listOf(
        "debited", "credited", "spent", "withdrawn", "deducted", "paid", "sent",
        "received", "transferred", "purchase", "txn", "transaction",
    )

    /** The balance in paise, or null when the message states none. */
    fun parseBalanceInPaise(rawText: String?): Long? {
        if (rawText.isNullOrBlank()) return null
        for (pattern in PATTERNS) {
            val digits = pattern.find(rawText)?.groupValues?.getOrNull(1) ?: continue
            val value = digits.replace(",", "").toDoubleOrNull() ?: continue
            // Paise as an integer, like every other amount in the app. Floating point never reaches
            // storage; the rounding is only to absorb the double's own representation error.
            return Math.round(value * 100)
        }
        return null
    }

    /**
     * True when the message states a balance and nothing moved.
     *
     * Used to route these to a balance-only side path instead of discarding them, so the reconcile
     * nudge has something to work with even in a month with no captured transactions.
     */
    fun isBalanceOnly(rawText: String?): Boolean {
        if (rawText.isNullOrBlank()) return false
        if (parseBalanceInPaise(rawText) == null) return false
        val text = rawText.lowercase()
        return MOVEMENT_WORDS.none { text.contains(it) }
    }
}
