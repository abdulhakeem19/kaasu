package com.kaasu.app.notification.filter

/**
 * Recognises a credit-card bill payment.
 *
 * This is the other half of the double-count. A card purchase is a real expense on the card; paying
 * that card's bill is not a second purchase, it is money moving from savings to the card. Counting
 * both turned one ₹2,399 subscription into ₹4,798.
 *
 * Detection is deliberately conservative — a false positive silently erases a genuine expense from
 * the month's total, which is worse than missing one. Every phrasing here is tied to a card being
 * *settled*, not merely used. Phrasings are drawn from how Indian issuers and BBPS/CRED actually
 * word it; matching is on lowercased text, so all patterns are lowercase.
 */
object CreditCardBillDetector {

    private val BILL_PHRASES = listOf(
        "payment received",
        "payment recd",
        "we have received your payment",
        "payment towards your credit card",
        "payment towards your card",
        "credit card bill",
        "card bill paid",
        "bill payment",
        "billpay",
        "bbps",
        "autopay",
        "auto pay towards",
        "cred",
        "towards your card ending",
        "thank you for your payment",
        "payment of rs",
    )

    /** A card being *used* rather than settled — these must never read as a bill payment. */
    private val PURCHASE_PHRASES = listOf(
        "spent",
        "purchase",
        "transaction of",
        "debited for",
        "swiped",
        "has been used",
    )

    private val CARD_PHRASES = listOf("credit card", "card ending", "card no", "cc ", " card")

    /**
     * True when [rawText] reads as settling a card rather than spending on one.
     *
     * Requires both a settlement phrase and some mention of a card, and refuses anything that also
     * carries purchase language — "spent on your credit card, payment of Rs 500 due" is a purchase
     * notice, not a bill payment.
     */
    fun isBillPayment(rawText: String?): Boolean {
        if (rawText.isNullOrBlank()) return false
        val text = rawText.lowercase()

        // "cred" is a substring of "credit", "credited" and "credential"; require it standalone.
        fun mentions(phrase: String) = when (phrase) {
            "cred" -> Regex("\\bcred\\b").containsMatchIn(text)
            else -> text.contains(phrase)
        }

        if (PURCHASE_PHRASES.any { text.contains(it) }) return false
        if (!BILL_PHRASES.any { mentions(it) }) return false
        return CARD_PHRASES.any { text.contains(it) }
    }
}
