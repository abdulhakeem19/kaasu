package com.kaasu.app.domain.usecase.subscription

import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectSubscriptionsUseCaseTest {

    private val useCase = DetectSubscriptionsUseCase()
    private val day = Subscription.DAY_MS
    private val now = 100L * day // a fixed "now" so day math is deterministic

    private fun tx(
        merchant: String?,
        amountPaise: Long,
        daysAgo: Long,
        type: TransactionType = TransactionType.EXPENSE,
        recurring: Boolean = false
    ) = Transaction(
        amountInPaise = amountPaise,
        type = type,
        merchantName = merchant,
        categoryId = null,
        sourceAppPackage = null,
        sourceAppName = null,
        paymentMode = null,
        rawTextHash = null,
        transactionTime = now - daysAgo * day,
        createdAt = 0, updatedAt = 0,
        note = null,
        isRecurring = recurring
    )

    @Test fun detects_monthly_same_amount() {
        val txns = listOf(
            tx("Netflix", 64900, daysAgo = 60),
            tx("Netflix", 64900, daysAgo = 30),
            tx("Netflix", 64900, daysAgo = 0),
        )
        val subs = useCase(txns, now)
        assertEquals(1, subs.size)
        assertEquals("Netflix", subs[0].merchantName)
        assertEquals(64900, subs[0].typicalAmountInPaise)
        assertEquals(3, subs[0].chargeCount)
    }

    @Test fun ignores_one_off_purchase() {
        val txns = listOf(tx("Amazon", 120000, daysAgo = 5))
        assertTrue(useCase(txns, now).isEmpty())
    }

    @Test fun ignores_irregular_varying_amounts() {
        // Same merchant but wildly different amounts and non-monthly spacing → not a subscription
        val txns = listOf(
            tx("Swiggy", 30000, daysAgo = 40),
            tx("Swiggy", 95000, daysAgo = 38),
            tx("Swiggy", 12000, daysAgo = 2),
        )
        assertTrue(useCase(txns, now).isEmpty())
    }

    @Test fun manual_flag_makes_subscription_from_single_charge() {
        val txns = listOf(tx("Gym", 150000, daysAgo = 10, recurring = true))
        val subs = useCase(txns, now)
        assertEquals(1, subs.size)
        assertTrue(subs[0].isManuallyFlagged)
    }

    // Regression: a transaction marked recurring must appear even if its type is UNKNOWN
    // (e.g. "youtubegoogle" the parser couldn't resolve), not only EXPENSE.
    @Test fun manual_flag_on_unknown_type_appears() {
        val txns = listOf(tx("youtubegoogle", 12900, daysAgo = 3, type = TransactionType.UNKNOWN, recurring = true))
        val subs = useCase(txns, now)
        assertEquals(1, subs.size)
        assertEquals("youtubegoogle", subs[0].merchantName)
        assertTrue(subs[0].isManuallyFlagged)
    }

    @Test fun flags_unused_after_60_days() {
        val txns = listOf(
            tx("Audible", 19900, daysAgo = 103),
            tx("Audible", 19900, daysAgo = 73),
        )
        val subs = useCase(txns, now)
        assertEquals(1, subs.size)
        assertTrue(subs[0].isLikelyUnused)
    }

    @Test fun income_is_never_a_subscription() {
        val txns = listOf(
            tx("Salary", 8500000, daysAgo = 60, type = TransactionType.INCOME),
            tx("Salary", 8500000, daysAgo = 30, type = TransactionType.INCOME),
        )
        assertTrue(useCase(txns, now).isEmpty())
    }
}
