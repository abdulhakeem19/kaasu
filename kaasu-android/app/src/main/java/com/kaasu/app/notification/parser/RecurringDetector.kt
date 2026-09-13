package com.kaasu.app.notification.parser

/**
 * Flags transactions that look like a recurring charge — autopay / e-mandate executions, standing
 * instructions, SIPs, EMIs. Used to auto-mark captured transactions as recurring so they surface in
 * Subscriptions without the user manually flagging them.
 *
 * Only applied to real captured transactions (the parser has already confirmed amount + direction),
 * so these markers indicate an actual recurring debit, not a setup reminder.
 */
object RecurringDetector {

    fun isRecurring(text: String): Boolean {
        val lower = text.lowercase()
        return MARKERS.any { lower.contains(it) }
    }

    private val MARKERS = listOf(
        "autopay", "auto pay", "auto-debit", "auto debit", "autodebit",
        "e-mandate", "emandate", "mandate executed", "standing instruction",
        "recurring", "sip ", "systematic investment", "emi",
        "subscription", "auto-renew", "auto renew",
    )
}
