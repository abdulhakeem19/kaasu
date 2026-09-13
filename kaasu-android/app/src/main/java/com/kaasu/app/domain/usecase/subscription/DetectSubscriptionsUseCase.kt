package com.kaasu.app.domain.usecase.subscription

import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.model.TransactionType
import javax.inject.Inject
import kotlin.math.abs

/**
 * A merchant Kaasu believes you pay on a repeating schedule.
 */
data class Subscription(
    val merchantName: String,
    val typicalAmountInPaise: Long,
    val firstChargedAt: Long,
    val lastChargedAt: Long,
    val estimatedNextChargeAt: Long,
    val cadenceDays: Int,
    val chargeCount: Int,
    val totalSpentInPaise: Long,
    val isManuallyFlagged: Boolean,
    val daysSinceLastCharge: Int
) {
    val daysUntilRenewal: Int get() {
        val diff = estimatedNextChargeAt - System.currentTimeMillis()
        return Math.ceil(diff / DAY_MS.toDouble()).toInt()
    }

    // No new charge for ~2 cadences (and at least 60 days) → likely unused.
    val isLikelyUnused: Boolean get() = daysSinceLastCharge >= UNUSED_DAYS

    companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
        const val UNUSED_DAYS = 60
    }
}

/**
 * Detects subscriptions from transaction history. A merchant qualifies when either:
 *  - it has ≥2 expense charges of similar amount spaced roughly a month apart, OR
 *  - any of its transactions is manually flagged `isRecurring`.
 *
 * Pure function over a transaction list so it is unit-testable; the ViewModel feeds it the flow.
 */
class DetectSubscriptionsUseCase @Inject constructor() {

    operator fun invoke(transactions: List<Transaction>, now: Long = System.currentTimeMillis()): List<Subscription> {
        // Candidates: any spend-like transaction with a merchant. Crucially this includes UNKNOWN
        // and TRANSFER (and anything manually flagged recurring) — a subscription marked by the user
        // must show up even if the parser couldn't resolve its direction. Income/refund/cashback
        // are never subscriptions.
        val expenses = transactions.filter {
            !it.merchantName.isNullOrBlank() &&
                it.type != TransactionType.INCOME &&
                it.type != TransactionType.REFUND &&
                it.type != TransactionType.CASHBACK
        }
        val byMerchant = expenses.groupBy { it.merchantName!!.trim().lowercase() }

        val subs = mutableListOf<Subscription>()
        for ((_, group) in byMerchant) {
            val sorted = group.sortedBy { it.transactionTime }
            val displayName = sorted.last().merchantName!!.trim()
            val manuallyFlagged = group.any { it.isRecurring }

            val gaps = sorted.zipWithNext { a, b ->
                ((b.transactionTime - a.transactionTime) / Subscription.DAY_MS).toInt()
            }
            val monthlyGaps = gaps.filter { it in MIN_CADENCE..MAX_CADENCE }
            val looksRecurring = monthlyGaps.size >= 1 && sorted.size >= 2 && amountsSimilar(sorted)

            if (!manuallyFlagged && !looksRecurring) continue

            val cadence = if (monthlyGaps.isNotEmpty()) monthlyGaps.average().toInt() else 30
            val last = sorted.last()
            val daysSince = ((now - last.transactionTime) / Subscription.DAY_MS).toInt()

            subs += Subscription(
                merchantName = displayName,
                typicalAmountInPaise = sorted.map { it.amountInPaise }.sorted()[sorted.size / 2], // median
                firstChargedAt = sorted.first().transactionTime,
                lastChargedAt = last.transactionTime,
                estimatedNextChargeAt = last.transactionTime + cadence * Subscription.DAY_MS,
                cadenceDays = cadence,
                chargeCount = sorted.size,
                totalSpentInPaise = sorted.sumOf { it.amountInPaise },
                isManuallyFlagged = manuallyFlagged,
                daysSinceLastCharge = daysSince
            )
        }
        return subs.sortedBy { it.estimatedNextChargeAt }
    }

    // Charges count as "the same" subscription if they're within 20% of the median amount.
    private fun amountsSimilar(txns: List<Transaction>): Boolean {
        val amounts = txns.map { it.amountInPaise }.sorted()
        val median = amounts[amounts.size / 2]
        if (median <= 0) return false
        return amounts.all { abs(it - median).toDouble() / median <= 0.20 }
    }

    companion object {
        private const val MIN_CADENCE = 26 // days — tolerate short months / early charges
        private const val MAX_CADENCE = 35
    }
}
