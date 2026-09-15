package com.kaasu.app.domain.money

import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.domain.model.TransferRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendRulesTest {

    private fun tx(
        type: TransactionType,
        amountInPaise: Long = 10_000L,
        isIgnored: Boolean = false,
        isDuplicate: Boolean = false,
        transferRole: TransferRole? = null,
    ) = Transaction(
        id = 0,
        amountInPaise = amountInPaise,
        currency = "INR",
        type = type,
        merchantName = "Test",
        categoryId = null,
        sourceAppPackage = null,
        sourceAppName = null,
        paymentMode = null,
        rawTextHash = null,
        confidenceScore = 100,
        transactionTime = 0L,
        createdAt = 0L,
        updatedAt = 0L,
        isManual = false,
        isTransfer = type == TransactionType.TRANSFER,
        isRefund = type == TransactionType.REFUND,
        isIgnored = isIgnored,
        note = null,
        accountId = null,
        isRecurring = false,
        parentId = null,
        isDuplicate = isDuplicate,
        transferRole = transferRole,
    )

    // ── The bug this object exists to kill ────────────────────────────────────

    @Test
    fun `a transfer is not spending`() {
        // Moving money between two of your own accounts is not a purchase. Counting it was what
        // turned one ₹2,399 card bill into ₹4,798 of apparent spending.
        assertFalse(SpendRules.isSpend(tx(TransactionType.TRANSFER)))
    }

    @Test
    fun `a card purchase plus paying that card bill counts the money once`() {
        val cardPurchase = tx(TransactionType.EXPENSE, 239_900L)
        val billPayment = tx(TransactionType.TRANSFER, 239_900L) // savings → credit card
        assertEquals(239_900L, SpendRules.netSpendInPaise(listOf(cardPurchase, billPayment)))
    }

    // ── Counting ──────────────────────────────────────────────────────────────

    @Test
    fun `an ignored row counts for nothing`() {
        assertFalse(SpendRules.isCounted(tx(TransactionType.EXPENSE, isIgnored = true)))
        assertFalse(SpendRules.isSpend(tx(TransactionType.EXPENSE, isIgnored = true)))
    }

    @Test
    fun `a row flagged as a duplicate counts for nothing`() {
        assertFalse(SpendRules.isSpend(tx(TransactionType.EXPENSE, isDuplicate = true)))
    }

    @Test
    fun `an unparsed row is neither spend nor income`() {
        val unknown = tx(TransactionType.UNKNOWN)
        assertFalse(SpendRules.isSpend(unknown))
        assertFalse(SpendRules.isIncome(unknown))
        assertEquals(0L, SpendRules.netSpendInPaise(listOf(unknown)))
    }

    // ── Refunds ───────────────────────────────────────────────────────────────

    @Test
    fun `a refund reduces spending rather than counting as income`() {
        val bought = tx(TransactionType.EXPENSE, 100_000L)
        val returned = tx(TransactionType.REFUND, 40_000L)
        assertEquals(60_000L, SpendRules.netSpendInPaise(listOf(bought, returned)))
        assertEquals(0L, SpendRules.totalIncomeInPaise(listOf(bought, returned)))
    }

    @Test
    fun `cashback reduces spending too`() {
        val bought = tx(TransactionType.EXPENSE, 100_000L)
        val cashback = tx(TransactionType.CASHBACK, 5_000L)
        assertEquals(95_000L, SpendRules.netSpendInPaise(listOf(bought, cashback)))
    }

    @Test
    fun `a month with more refunds than purchases reads as money back, not zero`() {
        // Deliberately not floored: a ₹5,000 refund against ₹1,000 of purchases genuinely ended
        // the month ahead, and showing ₹0 would hide that. Only progress bars floor, at display.
        val bought = tx(TransactionType.EXPENSE, 100_000L)
        val refunded = tx(TransactionType.REFUND, 500_000L)
        assertEquals(-400_000L, SpendRules.netSpendInPaise(listOf(bought, refunded)))
    }

    // ── Outflow is a display rule, not an aggregation rule ────────────────────

    @Test
    fun `a transfer displays as an outflow even though it is not spend`() {
        val transfer = tx(TransactionType.TRANSFER)
        assertTrue(SpendRules.isOutflow(transfer))
        assertFalse(SpendRules.isSpend(transfer))
    }

    @Test
    fun `only the leg the money left displays as an outflow`() {
        val out = tx(TransactionType.TRANSFER, transferRole = TransferRole.OUT)
        val into = tx(TransactionType.TRANSFER, transferRole = TransferRole.IN)
        assertTrue(SpendRules.isOutflow(out))
        assertFalse("the receiving account gained the money, it did not lose it", SpendRules.isOutflow(into))
    }

    @Test
    fun `a transfer from before roles existed still displays as an outflow`() {
        // Legacy rows have no role; the old behaviour is the safer default for them.
        assertTrue(SpendRules.isOutflow(tx(TransactionType.TRANSFER, transferRole = null)))
    }

    @Test
    fun `income never displays as an outflow`() {
        assertFalse(SpendRules.isOutflow(tx(TransactionType.INCOME)))
        assertFalse(SpendRules.isOutflow(tx(TransactionType.REFUND)))
    }

    // ── Income ────────────────────────────────────────────────────────────────

    @Test
    fun `salary is income`() {
        assertTrue(SpendRules.isIncome(tx(TransactionType.INCOME)))
        assertEquals(10_000L, SpendRules.totalIncomeInPaise(listOf(tx(TransactionType.INCOME))))
    }

    // ── The account filter must not lose money ────────────────────────────────

    @Test
    fun `per-account totals add back up to the unfiltered total`() {
        // The dashboard's account filter partitions by accountId. Rows with no account are the easy
        // ones to forget, and forgetting them makes the parts quietly add up to less than the whole.
        val rows = listOf(
            tx(TransactionType.EXPENSE, 100_000L).copy(accountId = 1L),
            tx(TransactionType.EXPENSE, 250_000L).copy(accountId = 2L),
            tx(TransactionType.REFUND, 40_000L).copy(accountId = 1L),
            tx(TransactionType.TRANSFER, 500_000L).copy(accountId = 1L),
            tx(TransactionType.EXPENSE, 30_000L).copy(accountId = null),
        )

        val whole = SpendRules.netSpendInPaise(rows)
        val parts = rows.map { it.accountId }.distinct()
            .sumOf { id -> SpendRules.netSpendInPaise(rows.filter { it.accountId == id }) }

        assertEquals(whole, parts)
        assertEquals(340_000L, whole)
    }

    @Test
    fun `an empty month totals zero both ways`() {
        assertEquals(0L, SpendRules.netSpendInPaise(emptyList()))
        assertEquals(0L, SpendRules.totalIncomeInPaise(emptyList()))
    }
}
