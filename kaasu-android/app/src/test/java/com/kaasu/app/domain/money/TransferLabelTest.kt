package com.kaasu.app.domain.money

import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.domain.model.TransferRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransferLabelTest {

    private fun transfer(role: TransferRole?, groupId: String? = "g1") = Transaction(
        id = 1, amountInPaise = 239_900L, currency = "INR", type = TransactionType.TRANSFER,
        merchantName = null, categoryId = null, sourceAppPackage = null, sourceAppName = null,
        paymentMode = null, rawTextHash = null, confidenceScore = 100, transactionTime = 0L,
        createdAt = 0L, updatedAt = 0L, isManual = false, isTransfer = true, isRefund = false,
        isIgnored = false, note = null, accountId = 1L, isRecurring = false, parentId = null,
        isDuplicate = false, transferGroupId = groupId, transferRole = role,
        counterpartAccountId = 2L,
    )

    @Test fun `money leaving reads from this account to the other`() {
        assertEquals(
            "Union Bank → SBI Card",
            TransferLabel.of(transfer(TransferRole.OUT), "Union Bank", "SBI Card"),
        )
    }

    @Test fun `money arriving reads from the other account to this one`() {
        assertEquals(
            "IDFC FIRST → Union Bank",
            TransferLabel.of(transfer(TransferRole.IN), "Union Bank", "IDFC FIRST"),
        )
    }

    @Test fun `a one-legged group names the direction rather than inventing a destination`() {
        // Normal, not broken: a credit card rarely announces that its bill was paid.
        assertEquals("Out of Union Bank", TransferLabel.of(transfer(TransferRole.OUT), "Union Bank", null))
        assertEquals("Into Union Bank", TransferLabel.of(transfer(TransferRole.IN), "Union Bank", null))
    }

    @Test fun `a transfer with no role at all still says what it is`() {
        assertEquals("Transfer · Union Bank", TransferLabel.of(transfer(null), "Union Bank", null))
        assertEquals("Transfer", TransferLabel.of(transfer(null), null, null))
    }

    @Test fun `an ordinary payment gets no transfer label`() {
        val expense = transfer(null, groupId = null).copy(
            type = TransactionType.EXPENSE, isTransfer = false, merchantName = "SWIGGY",
        )
        assertNull("so the caller falls back to the merchant name", TransferLabel.of(expense, "Union Bank", null))
    }

    @Test fun `a legacy transfer with no group is still labelled`() {
        val legacy = transfer(null, groupId = null)
        assertEquals("Transfer · Union Bank", TransferLabel.of(legacy, "Union Bank", null))
    }

    @Test fun `blank account names are treated as missing`() {
        assertEquals("Transfer out", TransferLabel.of(transfer(TransferRole.OUT), "  ", ""))
    }
}
