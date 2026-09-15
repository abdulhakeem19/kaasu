package com.kaasu.app.domain.money

import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.domain.model.TransferRole
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The sign convention for balances — the mirror of [SpendRules.isSpend].
 *
 * Spending asks whether money left the owner's hands; a balance asks whether money left *this
 * account*, which a transfer very much does, on both ends and in opposite directions.
 */
class SignedDeltaTest {

    private fun tx(
        type: TransactionType,
        amountInPaise: Long = 10_000L,
        transferRole: TransferRole? = null,
        isIgnored: Boolean = false,
        isDuplicate: Boolean = false,
        accountId: Long? = 1L,
    ) = Transaction(
        id = 0, amountInPaise = amountInPaise, currency = "INR", type = type,
        merchantName = null, categoryId = null, sourceAppPackage = null, sourceAppName = null,
        paymentMode = null, rawTextHash = null, confidenceScore = 100, transactionTime = 0L,
        createdAt = 0L, updatedAt = 0L, isManual = false,
        isTransfer = type == TransactionType.TRANSFER, isRefund = false,
        isIgnored = isIgnored, note = null, accountId = accountId, isRecurring = false,
        parentId = null, isDuplicate = isDuplicate, transferGroupId = null,
        transferRole = transferRole, counterpartAccountId = null,
    )

    @Test fun `spending reduces the balance`() {
        assertEquals(-10_000L, SpendRules.signedDeltaInPaise(tx(TransactionType.EXPENSE)))
    }

    @Test fun `income raises it`() {
        assertEquals(10_000L, SpendRules.signedDeltaInPaise(tx(TransactionType.INCOME)))
    }

    @Test fun `a refund puts the money back`() {
        assertEquals(10_000L, SpendRules.signedDeltaInPaise(tx(TransactionType.REFUND)))
        assertEquals(10_000L, SpendRules.signedDeltaInPaise(tx(TransactionType.CASHBACK)))
    }

    @Test fun `the two legs of a transfer move in opposite directions`() {
        assertEquals(-10_000L, SpendRules.signedDeltaInPaise(tx(TransactionType.TRANSFER, transferRole = TransferRole.OUT)))
        assertEquals(10_000L, SpendRules.signedDeltaInPaise(tx(TransactionType.TRANSFER, transferRole = TransferRole.IN)))
    }

    @Test fun `a transfer group nets to zero across all accounts`() {
        // The property the whole design rests on: money moved between the owner's own accounts
        // changes where it is, never how much there is.
        val out = tx(TransactionType.TRANSFER, 239_900L, TransferRole.OUT, accountId = 1L)
        val into = tx(TransactionType.TRANSFER, 239_900L, TransferRole.IN, accountId = 2L)
        assertEquals(0L, listOf(out, into).sumOf(SpendRules::signedDeltaInPaise))
    }

    @Test fun `a transfer leg with no role moves nothing`() {
        // Guessing a direction would move money the wrong way, which is worse than not moving it.
        assertEquals(0L, SpendRules.signedDeltaInPaise(tx(TransactionType.TRANSFER, transferRole = null)))
    }

    @Test fun `rows that count for nothing move nothing`() {
        assertEquals(0L, SpendRules.signedDeltaInPaise(tx(TransactionType.EXPENSE, isIgnored = true)))
        assertEquals(0L, SpendRules.signedDeltaInPaise(tx(TransactionType.EXPENSE, isDuplicate = true)))
        assertEquals(0L, SpendRules.signedDeltaInPaise(tx(TransactionType.UNKNOWN)))
    }

    @Test fun `a card purchase and paying its bill leave the card settled`() {
        // On the card: the purchase, then the arriving bill payment. Net zero — nothing outstanding.
        val purchase = tx(TransactionType.EXPENSE, 239_900L, accountId = 9L)
        val billArriving = tx(TransactionType.TRANSFER, 239_900L, TransferRole.IN, accountId = 9L)
        assertEquals(0L, listOf(purchase, billArriving).sumOf(SpendRules::signedDeltaInPaise))
    }
}
