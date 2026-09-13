package com.kaasu.app.notification.parser

import com.kaasu.app.domain.model.TransactionType

object TransactionTypeParser {

    // Called by TransactionParser.parse(); result is forwarded to MerchantParser.parse() to pick "to" vs "from" pattern
    fun parse(text: String): TransactionType {
        val lower = text.lowercase()
        return when {
            CASHBACK_KEYWORDS.any { lower.contains(it) }         -> TransactionType.CASHBACK
            REFUND_KEYWORDS.any { lower.contains(it) }           -> TransactionType.REFUND
            // Explicit self-transfers carry a bare "paid"/"sent" debit keyword but are not expenses.
            // Only unambiguous markers are used here: "to your account" is deliberately NOT one,
            // because "credited to your account" is an income, not a transfer.
            TRANSFER_KEYWORDS.any { lower.contains(it) } ||
                SELF_TRANSFER_PHRASES.any { lower.contains(it) } -> TransactionType.TRANSFER
            // Directional income phrases before generic DEBIT: "paid you" = income (someone paid you),
            // whereas bare "paid" = expense (you paid someone). Must be checked before DEBIT_KEYWORDS.
            INCOME_PHRASES.any { lower.contains(it) }            -> TransactionType.INCOME
            // DEBIT before CREDIT: UPI P2P debits say "A/c debited Rs.X; Recipient credited" —
            // checking CREDIT first would misclassify the debit as INCOME.
            DEBIT_KEYWORDS.any { lower.contains(it) }            -> TransactionType.EXPENSE
            CREDIT_KEYWORDS.any { lower.contains(it) }           -> TransactionType.INCOME
            else                                                  -> TransactionType.UNKNOWN
        }
    }

    private val DEBIT_KEYWORDS = listOf(
        "debited", "debit", "paid", "payment", "sent", "withdrawn",
        "spent", "purchase", "charged", "deducted", "used"
    )
    // "credit" is intentionally absent — "Credit Card" is a payment instrument, not income.
    // Only "credited" (past tense) reliably signals an incoming transaction.
    private val CREDIT_KEYWORDS = listOf(
        "credited", "received", "deposited", "added"
    )
    // High-priority directional phrases that override bare debit keywords
    private val INCOME_PHRASES = listOf("paid you", "sent you")
    private val REFUND_KEYWORDS = listOf("refund", "reversed", "returned")
    private val CASHBACK_KEYWORDS = listOf("cashback", "cash back")
    private val TRANSFER_KEYWORDS = listOf("transferred", "transfer")
    private val SELF_TRANSFER_PHRASES = listOf("to self", "self transfer", "self-transfer")
}
