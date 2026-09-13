package com.kaasu.app.notification.filter

/**
 * Drops promotional/offer notifications that happen to contain a rupee amount but describe no
 * actual money movement (e.g. "Get ₹100 cashback, use code SAVE100" or "Flat 50% off, shop now").
 *
 * Called by [com.kaasu.app.notification.parser.TransactionParser] before any amount/type parsing.
 * This lives in code (not only in the seeded `ignored_patterns` table) so existing installs are
 * protected without a database migration.
 *
 * Rule: a notification is promotional iff it carries a promo marker AND no transactional-confirmation
 * signal. The signal escape hatch is what keeps genuine receipts — including real cashback receipts
 * like "₹10 cashback credited to A/c XX1234" — from being discarded.
 */
object PromotionalDetector {

    fun isPromotional(text: String): Boolean {
        val lower = text.lowercase()
        val hasPromoMarker = PROMO_MARKERS.any { lower.contains(it) }
        if (!hasPromoMarker) return false
        return !hasTransactionalSignal(lower, text)
    }

    private fun hasTransactionalSignal(lower: String, original: String): Boolean {
        if (TRANSACTIONAL_SIGNALS.any { lower.contains(it) }) return true
        return CARD_ENDING_PATTERN.containsMatchIn(original)
    }

    // Advertising language. Broad on purpose — the transactional-signal escape hatch prevents
    // these from dropping real receipts that merely mention an offer.
    private val PROMO_MARKERS = listOf(
        "% off", "upto", "up to", "flat ", "use code", "coupon", "voucher",
        "cashback", "win ", "earn ", "claim",
        "hurry", "limited period", "limited time", "apply now", "eligible",
        "get ₹", "get rs", "save up", "lowest price", "best price", "sale",
        "deal", "exclusive offer", "special offer", "offer", "discount",
        "shop now", "buy now", "stock up", "for you", "tap to get",
        "pre-approved", "loan offer", "instant loan",
        "reward points", "no cost emi", "convert to emi"
    )

    // Receipt cues. Presence of any one means a real transaction occurred, so the notification
    // is kept even if it also advertises an offer (e.g. a real "₹10 cashback credited to A/c …").
    private val TRANSACTIONAL_SIGNALS = listOf(
        "debited", "credited", "paid to", "received from", "sent to",
        "spent", "withdrawn", "deducted", "added to", "a/c", "acct",
        "upi ref", "ref no", "refno", "txn id", "transaction id",
        "avl bal", "avbl bal", "available balance"
    )

    // Masked card/account tails such as "xx1234", "xxxx1234", "ending 1234".
    private val CARD_ENDING_PATTERN = Regex("""(?:x{2,}|ending\s+)\d{3,4}""", RegexOption.IGNORE_CASE)
}
