package com.kaasu.app.notification.filter

/**
 * Drops notifications that *announce* a payment rather than confirm one — UPI mandate pre-debit
 * notices, scheduled autopay reminders, and bill-due nudges.
 *
 * Called by [com.kaasu.app.notification.parser.TransactionParser] right after
 * [PromotionalDetector], before any amount parsing.
 *
 * Why this matters more than it looks: these notices are always followed by a *real* confirmation
 * for the same mandate, and the two arrive days apart, so [com.kaasu.app.notification.duplicate.DuplicateChecker]'s
 * time window can never pair them. Storing both double-counts the spend. On the device sample this
 * was built from, one ₹300 mandate had 20 pre-debit notices stored against 3 actual debits.
 *
 * The rule is deliberately tense-based, not keyword-based: only wording that is unambiguously about
 * money that has NOT moved yet. "Debited" and "was successful" describe completed movement and are
 * never matched here.
 */
object PendingPaymentDetector {

    fun isPending(text: String): Boolean {
        val lower = text.lowercase()
        return PENDING_MARKERS.any { lower.contains(it) }
    }

    // Every entry here describes a future or requested payment. Kept narrow on purpose — a false
    // positive silently loses a real transaction, which is worse than the double-count it prevents.
    private val PENDING_MARKERS = listOf(
        // UPI mandate pre-debit notice ("Your account will be debited with Rs 300 towards X")
        "will be debited",
        "will be deducted",
        "will be charged",
        "pause mandate",
        // Scheduled / upcoming autopay ("Autopay payment of ₹872.10 to X is scheduled for Jul 1")
        "is scheduled for",
        "upcoming autopay",
        "scheduled for",
        // Bill and payment reminders ("Payment reminder for House rent  Pay X ₹11500 today")
        "payment reminder",
        "reminder to pay",
        "is due on",
        "due date",
        "please pay",
        // Balance nudges that accompany the above
        "sufficient balance",
        // Outcomes that mean no money moved. Google Pay lists these in the same row shape as a
        // successful payment — "Payment to X chumma ₹10 Failed … Your money was not debited" was
        // stored as a ₹10 expense.
        "was not debited",
        "money was not deducted",
        "payment failed",
        "transaction failed",
        "failed •",
        "pending •",
        "cancelled •",
    )
}
