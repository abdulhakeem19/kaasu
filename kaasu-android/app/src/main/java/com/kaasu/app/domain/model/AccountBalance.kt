package com.kaasu.app.domain.model

/**
 * An account with its balance worked out, plus whatever the bank last claimed it was.
 *
 * The balance is derived on every read rather than stored. See [com.kaasu.app.domain.usecase.account.GetAccountBalancesUseCase].
 */
data class AccountBalance(
    val account: Account?,
    val balanceInPaise: Long,
    /** Rows counted into [balanceInPaise] — what "since you set the opening balance" covers. */
    val transactionCount: Int,
) {
    /**
     * Whether the figure means anything yet.
     *
     * Without an opening balance the sum of known transactions is not a balance, it is just the
     * movement Kaasu happened to see. Showing it as ₹0 would be a confident lie; the UI asks for the
     * opening balance instead.
     */
    val isKnown: Boolean get() = account?.hasOpeningBalance == true

    /** Money owed reads as a positive outstanding amount, not a negative balance. */
    val isCreditCard: Boolean get() = account?.accountType == AccountType.CREDIT_CARD

    val outstandingInPaise: Long get() = if (balanceInPaise < 0) -balanceInPaise else 0L

    /**
     * The gap between what Kaasu worked out and what the bank last said, or null when there is
     * nothing to compare. Non-zero usually means a message Kaasu never saw.
     */
    val driftInPaise: Long?
        get() {
            val stated = account?.lastStatedBalanceInPaise ?: return null
            if (!isKnown) return null
            return balanceInPaise - stated
        }

    /** Transactions belonging to no account at all — money that would otherwise go uncounted. */
    val isUnassigned: Boolean get() = account == null
}
