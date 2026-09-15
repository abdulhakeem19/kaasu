package com.kaasu.app.domain.money

import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.model.TransferRole

/**
 * Builds "Union Bank → SBI Card" for a transfer row, at display time.
 *
 * The arrow used to be written into `merchantName` at capture, which made it unqueryable, destroyed
 * whatever the bank actually said, and froze the account names as they were spelled that day. The
 * direction now lives in the account ids, so the label is derived instead — rename an account and
 * every past transfer reads correctly.
 */
object TransferLabel {

    /**
     * Returns null when this is not a transfer, so callers can fall back to the merchant name.
     *
     * A group with only one leg is normal — a credit card rarely announces that its bill was paid —
     * so an unknown other end reads as a direction rather than pretending to a destination.
     */
    fun of(
        transaction: Transaction,
        accountName: String?,
        counterpartName: String?,
    ): String? {
        if (transaction.transferGroupId == null && !transaction.isTransfer) return null

        val here = accountName?.takeIf { it.isNotBlank() }
        val there = counterpartName?.takeIf { it.isNotBlank() }

        return when (transaction.transferRole) {
            TransferRole.OUT -> when {
                here != null && there != null -> "$here → $there"
                there != null -> "To $there"
                here != null -> "Out of $here"
                else -> "Transfer out"
            }
            TransferRole.IN -> when {
                here != null && there != null -> "$there → $here"
                there != null -> "From $there"
                here != null -> "Into $here"
                else -> "Transfer in"
            }
            // A transfer from before groups existed, or one whose role could not be established.
            null -> here?.let { "Transfer · $it" } ?: "Transfer"
        }
    }
}
