package com.kaasu.app.notification.filter

// Content-based "does this text look like a real bank/UPI transaction" check, shared by
// NotificationFilter (messaging-app notifications) and SmsFilter (raw SMS). Real bank SMS/notif
// text always carries a debit/credit verb; a bare currency symbol (as in "Sale! Flat ₹200 off")
// is deliberately NOT enough. This is a content gate only — sender/package allowlisting is
// handled separately by each caller.
object FinancialTextHeuristics {

    fun looksFinancial(text: String): Boolean {
        val lower = text.lowercase()
        return TRANSACTION_VERBS.any { lower.contains(it) } && AMOUNT_PATTERN.containsMatchIn(text)
    }

    // Past-tense / directional verbs that signal a real money movement. Imperative ad copy
    // ("pay", "get", "add ₹250", "buy") is intentionally excluded.
    private val TRANSACTION_VERBS = setOf(
        "debited", "credited", "spent", "withdrawn", "deducted",
        "paid to", "sent to", "received from", "transferred to"
    )
    private val AMOUNT_PATTERN = Regex("""[₹₨]|Rs\.?|INR""", RegexOption.IGNORE_CASE)
}
