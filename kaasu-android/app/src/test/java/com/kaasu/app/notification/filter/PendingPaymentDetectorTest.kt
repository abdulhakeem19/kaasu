package com.kaasu.app.notification.filter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingPaymentDetectorTest {

    @Test
    fun `idfc mandate pre-debit notice is pending`() {
        assertTrue(
            PendingPaymentDetector.isPending(
                "Your account will be debited with Rs 300.00 towards EXAMPLE SECURITIES LIMITED SI " +
                    "for the UPI Mandate on 28/11/2025. Pause mandate to stop execution. IDFC FIRST Bank"
            )
        )
    }

    @Test
    fun `gpay scheduled autopay is pending`() {
        assertTrue(
            PendingPaymentDetector.isPending(
                "Upcoming Autopay payment to Amazon Pay Autopay payment of Rs.872.10 to Amazon Pay " +
                    "is scheduled for Jul 1, 2026."
            )
        )
    }

    @Test
    fun `bill reminder is pending`() {
        assertTrue(PendingPaymentDetector.isPending("Payment reminder for House rent  Pay Shanthy Senthil Rs.11500.00 today"))
    }

    @Test
    fun `the completed debit for the same mandate is not pending`() {
        assertFalse(
            PendingPaymentDetector.isPending(
                "Your A/c XX1234 debited by Rs. 5.00 on 28/11/25; EXAMPLE SECURITIES LI credited. " +
                    "RRN 100000000001. Available balance Rs. 11.39."
            )
        )
    }

    @Test
    fun `completed autopay is not pending`() {
        assertFalse(PendingPaymentDetector.isPending("Payment for Autopay of Rs.835.44 to Hostinger was successful."))
    }

    @Test
    fun `ordinary upi debit is not pending`() {
        assertFalse(
            PendingPaymentDetector.isPending(
                "Union Bank of India A/c *0000 Debited Rs:115.00 on 12-06-2026, Fvg: ARUN TRAD Avl Bal Rs:1391.54"
            )
        )
    }
}
