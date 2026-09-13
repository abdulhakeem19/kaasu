package com.kaasu.app.notification.parser

import com.kaasu.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regression cases taken from real captured notifications that stored no merchant name.
 * Each string here is a shape that previously fell through every pattern and surfaced in the UI
 * as "Unknown"; amounts and account tails are edited, the sentence structure is verbatim.
 */
class MerchantParserRealFormatsTest {

    @Test
    fun `union bank Fvg beneficiary is captured and balance is not absorbed`() {
        val text = "Union Bank of India A/c *0000 Debited Rs:115.00 on 12-06-2026 20:24:09 " +
            "by Mob Bk ref no 600000000001, Fvg: ARUN TRAD Avl Bal Rs:1391.54"
        assertEquals("ARUN TRAD", MerchantParser.parse(text, TransactionType.EXPENSE))
    }

    @Test
    fun `union bank single word beneficiary`() {
        val text = "Union Bank of India A/c *0000 Debited Rs:10.00 on 13-06-2026 14:48:45 " +
            "by Mob Bk ref no 600000000002, Fvg: Selvaraj Avl Bal Rs:4915.04"
        assertEquals("Selvaraj", MerchantParser.parse(text, TransactionType.EXPENSE))
    }

    @Test
    fun `idfc mandate debit names the merchant after towards`() {
        val text = "Your account will be debited with Rs 300.00 towards EXAMPLE SECURITIES LIMITED SI " +
            "for the UPI Mandate on 28/11/2025. Pause mandate to stop execution. IDFC FIRST Bank"
        assertEquals("EXAMPLE SECURITIES LIMITED SI", MerchantParser.parse(text, TransactionType.EXPENSE))
    }

    @Test
    fun `idfc p2p debit names the beneficiary that precedes credited`() {
        val text = "Your A/c XX1234 debited by Rs. 5.00 on 28/11/25; EXAMPLE SECURITIES LI credited. " +
            "RRN 100000000001. Available balance Rs. 11.39. Team IDFC FIRST Bank"
        assertEquals("EXAMPLE SECURITIES LI", MerchantParser.parse(text, TransactionType.EXPENSE))
    }

    @Test
    fun `comma inside a thousands separated amount does not become the merchant`() {
        val text = "Your A/c XX1234 debited by Rs. 1,234.00 on 28/11/25; HOSTINGER INTL credited."
        assertEquals("HOSTINGER INTL", MerchantParser.parse(text, TransactionType.EXPENSE))
    }

    @Test
    fun `gpay autopay success names the payee`() {
        val text = "Payment successful Payment for Autopay of Rs.835.44 to Hostinger was successful. Tap to view."
        assertEquals("Hostinger", MerchantParser.parse(text, TransactionType.EXPENSE))
    }

    @Test
    fun `gpay scheduled autopay names the payee`() {
        val text = "Upcoming Autopay payment to Amazon Pay Autopay payment of Rs.872.10 to Amazon Pay " +
            "is scheduled for Jul 1, 2026."
        assertEquals("Amazon Pay", MerchantParser.parse(text, TransactionType.EXPENSE))
    }

    @Test
    fun `gpay refund credited to you still names the sender`() {
        val text = "RAVI KUMAR S paid you Rs.300.00 Bike repair Refund"
        assertEquals("RAVI KUMAR S", MerchantParser.parse(text, TransactionType.REFUND))
    }

    @Test
    fun `existing paid to format still wins over the new fallbacks`() {
        val text = "Paid to Swiggy Rs.245 via UPI. UPI Ref 123456789012"
        assertEquals("Swiggy", MerchantParser.parse(text, TransactionType.EXPENSE))
    }

    @Test
    fun `text with no beneficiary at all still yields null`() {
        val text = "Your A/c XX1234 debited by Rs. 5.00 on 28/11/25. Available balance Rs. 11.39."
        assertNull(MerchantParser.parse(text, TransactionType.EXPENSE))
    }
}
