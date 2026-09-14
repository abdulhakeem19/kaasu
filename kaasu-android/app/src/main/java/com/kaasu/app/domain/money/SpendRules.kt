package com.kaasu.app.domain.money

import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.model.TransactionType

/**
 * The single definition of what counts as spending.
 *
 * This existed as five divergent copies of `type == EXPENSE || type == TRANSFER` — in the dashboard,
 * budgets, reports (twice) and the needs-a-tag screen — plus two unused SQL aggregates that used a
 * sixth, different definition. Every one of them counted a transfer as spend, so moving money
 * between your own accounts inflated the total, and paying a credit-card bill counted the same
 * rupees twice: once when the card was charged, once when the bill was paid.
 *
 * Pure Kotlin on purpose: no Android imports, no database, so the rule is unit-testable on its own.
 * Deliberately not mirrored as a Room `@Query` — two implementations of one rule is exactly how the
 * five copies happened.
 */
object SpendRules {

    /**
     * Whether a row participates in totals at all.
     *
     * Ignored rows include split parents (the slices carry the money instead) and anything the
     * owner excluded by hand. Duplicates are flagged rather than deleted so they stay reversible.
     */
    fun isCounted(t: Transaction): Boolean = !t.isIgnored && !t.isDuplicate

    /**
     * Money genuinely leaving the owner's pocket.
     *
     * A transfer is excluded no matter which leg it is: money moved between two accounts the owner
     * holds has not been spent. That is also what makes a credit-card bill payment free — it is a
     * transfer into the card account, while the original purchase remains the real expense.
     */
    fun isSpend(t: Transaction): Boolean = isCounted(t) && t.type == TransactionType.EXPENSE

    /** Money coming in. Refunds are deliberately excluded — see [isReturn]. */
    fun isIncome(t: Transaction): Boolean = isCounted(t) && t.type == TransactionType.INCOME

    /**
     * Money given back for something already bought.
     *
     * Counted against spend rather than as income: treating a refund as income overstates both
     * sides, making a month look like it earned and spent more than it did.
     */
    fun isReturn(t: Transaction): Boolean =
        isCounted(t) && (t.type == TransactionType.REFUND || t.type == TransactionType.CASHBACK)

    /**
     * Whether a row should be *displayed* with a minus sign.
     *
     * Deliberately not the same as [isSpend]. Money leaving an account to another of the owner's
     * accounts is not spending, but it did leave, and a transaction list that showed it as a credit
     * would be lying about the direction. Totals use [isSpend]; rows use this.
     */
    fun isOutflow(t: Transaction): Boolean =
        isCounted(t) && (t.type == TransactionType.EXPENSE || t.type == TransactionType.TRANSFER)

    /**
     * Spend minus what came back.
     *
     * Not floored at zero: a month whose refunds exceed its purchases genuinely ended up ahead, and
     * showing ₹0 would hide that. Only a progress bar needs flooring, since a bar cannot be less
     * than empty — callers do that at the point of display.
     */
    fun netSpendInPaise(transactions: List<Transaction>): Long =
        transactions.sumOf { t ->
            when {
                isSpend(t) -> t.amountInPaise
                isReturn(t) -> -t.amountInPaise
                else -> 0L
            }
        }

    /** Total of [isIncome] rows. Refunds are not income; they reduce spend instead. */
    fun totalIncomeInPaise(transactions: List<Transaction>): Long =
        transactions.filter(::isIncome).sumOf { it.amountInPaise }
}
