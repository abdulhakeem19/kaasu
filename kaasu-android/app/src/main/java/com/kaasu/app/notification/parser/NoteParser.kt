package com.kaasu.app.notification.parser

/**
 * Extracts the free-text note the payer typed in GPay/PhonePe ("Bike repair", "Groceries",
 * "House rent") from a captured notification.
 *
 * These apps append the note after the amount with no label or delimiter — e.g.
 * "RAVI KUMAR S paid you ₹300.00 Bike repair" — so the note is simply whatever trails the
 * amount. That also means the app's own boilerplate trails the amount in exactly the same
 * position ("Tap to view.", "Sent using Paytm UPI"), which is why [BOILERPLATE] exists: without it
 * every note-less payment would be stored with a junk note.
 *
 * Returns null rather than a blank string so the caller can leave `note` untouched.
 */
object NoteParser {

    fun parse(text: String): String? {
        val match = TRAILING_AFTER_AMOUNT.find(text) ?: return null
        val candidate = match.groupValues[1]
            .trim()
            .trim('.', ',', '!', '?', ':', ';', '-', '_', '"', '\'', ' ')
            .replace(WHITESPACE, " ")

        if (candidate.length !in MIN_LENGTH..MAX_LENGTH) return null
        if (candidate.none { it.isLetter() }) return null

        // Strip a trailing boilerplate clause the payer did not type ("… Bike repair Tap to view.")
        var result = candidate
        for (phrase in BOILERPLATE) {
            val idx = result.lowercase().indexOf(phrase)
            if (idx >= 0) result = result.substring(0, idx).trim().trimEnd('.', ',', '-')
        }
        if (result.length < MIN_LENGTH) return null
        if (result.lowercase() in NON_NOTES) return null
        if (result.none { it.isLetter() }) return null
        return result
    }

    private const val MIN_LENGTH = 2
    private const val MAX_LENGTH = 60

    private val WHITESPACE = Regex("""\s+""")

    // The amount, then everything after it. Matches ₹/Rs./INR with optional thousands separators
    // so "₹5,000.00 Movie tickets" yields "Movie tickets" rather than "000.00 Movie tickets".
    private val TRAILING_AFTER_AMOUNT = Regex(
        """(?:₹|rs\.?|inr)\s*\d[\d,]*(?:\.\d{1,2})?\s+(.+)$""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    // App-generated clauses that occupy the same slot a typed note would.
    private val BOILERPLATE = listOf(
        "tap to view", "tap to", "sent using", "was successful", "is successful",
        "view details", "using paytm", "using google pay", "using phonepe",
        "upi ref", "ref no", "avl bal", "available balance", "not you?",
    )

    // Single words that are never a user-typed note, only leftover app copy.
    private val NON_NOTES = setOf("today", "successful", "completed", "paid", "received", "refund")
}
