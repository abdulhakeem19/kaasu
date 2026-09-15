package com.kaasu.app.notification.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BalanceParserTest {

    // ── Reading the figure ────────────────────────────────────────────────────

    @Test fun `avl bal with rupees and paise`() {
        assertEquals(1_198_050L, BalanceParser.parseBalanceInPaise(
            "Rs.2399.00 debited from A/c XX5678. Avl Bal: Rs.11,980.50"))
    }

    @Test fun `abbreviated avbl bal`() {
        assertEquals(123_400L, BalanceParser.parseBalanceInPaise("Avbl Bal Rs 1234"))
    }

    @Test fun `available balance spelled out`() {
        assertEquals(500_000L, BalanceParser.parseBalanceInPaise("Available Balance is INR 5,000.00"))
    }

    @Test fun `closing balance`() {
        assertEquals(9_950L, BalanceParser.parseBalanceInPaise("Closing balance: 99.50"))
    }

    @Test fun `a bare bal needs a currency marker`() {
        assertEquals(100_000L, BalanceParser.parseBalanceInPaise("Bal INR 1000"))
        // "bal 1000" alone is too weak a signal to trust.
        assertNull(BalanceParser.parseBalanceInPaise("bal 1000"))
    }

    @Test fun `the balance is read, not the transaction amount`() {
        // Both numbers are present and the debit comes first — the balance must still win.
        assertEquals(1_198_050L, BalanceParser.parseBalanceInPaise(
            "Rs.2399.00 debited from A/c XX5678 on 01-Sep. Avl Bal Rs.11,980.50"))
    }

    @Test fun `a message with no balance`() {
        assertNull(BalanceParser.parseBalanceInPaise("Rs.2399.00 debited from A/c XX5678 to ARUN STORES."))
        assertNull(BalanceParser.parseBalanceInPaise(null))
        assertNull(BalanceParser.parseBalanceInPaise(""))
    }

    @Test fun `paise are exact, not floated`() {
        // 0.07 is not representable in binary; rounding must still land on exactly 7 paise.
        assertEquals(7L, BalanceParser.parseBalanceInPaise("Avl Bal Rs 0.07"))
        assertEquals(1_234_567L, BalanceParser.parseBalanceInPaise("Avl Bal Rs 12,345.67"))
    }

    // ── Balance-only messages ─────────────────────────────────────────────────

    @Test fun `a pure balance enquiry reply is balance-only`() {
        assertTrue(BalanceParser.isBalanceOnly("Avl Bal in A/c XX5678 is Rs.11,980.50 as on 01-Sep-25."))
    }

    @Test fun `a debit that also states the balance is not balance-only`() {
        // It has to go through the normal path — money moved, and that row matters.
        assertFalse(BalanceParser.isBalanceOnly(
            "Rs.2399.00 debited from A/c XX5678. Avl Bal: Rs.11,980.50"))
    }

    @Test fun `a credit that also states the balance is not balance-only`() {
        assertFalse(BalanceParser.isBalanceOnly(
            "A/c XX5678 credited with Rs.50000. Avl Bal Rs.61,980.50"))
    }

    @Test fun `no balance means not balance-only`() {
        assertFalse(BalanceParser.isBalanceOnly("Your statement is ready."))
    }
}
