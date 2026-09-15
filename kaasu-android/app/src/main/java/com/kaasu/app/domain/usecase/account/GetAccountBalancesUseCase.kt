package com.kaasu.app.domain.usecase.account

import com.kaasu.app.domain.model.AccountBalance
import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.money.SpendRules
import com.kaasu.app.domain.repository.AccountRepository
import com.kaasu.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Works out each account's balance, and surfaces the money that belongs to no account.
 *
 * `balance = openingBalance + Σ signedDelta(rows on this account after openingBalanceAt)`.
 *
 * **Never persisted.** A stored running total has to be adjusted by every path that touches a
 * transaction — edit, delete, split, import, backfill, restore, dedup — and the first one that
 * forgets corrupts it silently and forever. Deriving it means a wrong balance always has a visible
 * cause in the rows, and fixing the rows fixes the balance.
 *
 * The sum is folded in Kotlin rather than written as a SQL aggregate on purpose: the sign convention
 * is [SpendRules.signedDeltaInPaise], and a second copy of it in SQL is precisely the mistake that
 * left five different definitions of "spend" disagreeing with each other.
 */
class GetAccountBalancesUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(): Flow<List<AccountBalance>> =
        combine(
            accountRepository.getAll(),
            transactionRepository.getAll(),
        ) { accounts, transactions ->
            val byAccount = transactions.groupBy { it.accountId }

            val balances = accounts.map { account ->
                // Only movement after the anchor counts — the opening balance already contains
                // everything before it, so including those rows would count them twice.
                val rows = byAccount[account.id]
                    ?.filter { tx ->
                        val from = account.openingBalanceAt
                        from == null || tx.transactionTime > from
                    }
                    .orEmpty()

                AccountBalance(
                    account = account,
                    balanceInPaise = (account.openingBalanceInPaise ?: 0L) + rows.sumOf(SpendRules::signedDeltaInPaise),
                    transactionCount = rows.size,
                )
            }

            // Money Kaasu captured but could not place: no last-four in the message, or the same
            // last-four at two banks. Without a bucket of its own it simply vanishes from the
            // account view, which reads as "you have less than you do".
            val orphans = byAccount[null].orEmpty()
            if (orphans.isEmpty()) {
                balances
            } else {
                balances + AccountBalance(
                    account = null,
                    balanceInPaise = orphans.sumOf(SpendRules::signedDeltaInPaise),
                    transactionCount = orphans.size,
                )
            }
        }

    /** The single figure for "how much do I have", excluding what is owed on cards. */
    fun netWorthInPaise(balances: List<AccountBalance>): Long =
        balances.filter { it.isKnown && !it.isCreditCard }.sumOf { it.balanceInPaise }
}
