package com.kaasu.app.notification.filter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The asymmetry here is deliberate: a missed bill payment overstates one month, while a false
 * positive silently erases a real purchase and never shows up as anything but a wrong total.
 */
class CreditCardBillDetectorTest {

    // ── Settling a card ───────────────────────────────────────────────────────

    @Test fun `payment received towards a card`() {
        assertTrue(CreditCardBillDetector.isBillPayment(
            "Payment received of Rs.2399.00 towards your credit card ending 1234."))
    }

    @Test fun `a bill payment from the paying bank`() {
        assertTrue(CreditCardBillDetector.isBillPayment(
            "Rs.2399.00 debited from A/c XX5678 towards your credit card bill payment."))
    }

    @Test fun `a BBPS bill payment`() {
        assertTrue(CreditCardBillDetector.isBillPayment(
            "BBPS: Rs 2399 paid to HDFC credit card ending 1234."))
    }

    @Test fun `CRED as a standalone word`() {
        assertTrue(CreditCardBillDetector.isBillPayment(
            "Rs.2399 paid via CRED towards your card ending 1234."))
    }

    @Test fun `autopay settling a card`() {
        assertTrue(CreditCardBillDetector.isBillPayment(
            "Autopay of Rs.2399.00 processed for your credit card ending 1234."))
    }

    // ── Using a card is not settling one ──────────────────────────────────────

    @Test fun `spending on the card is not a bill payment`() {
        assertFalse(CreditCardBillDetector.isBillPayment(
            "Rs.2399.00 spent on your SBI Credit Card ending 1234 at CLAUDE AI."))
    }

    @Test fun `a purchase notice that also mentions an amount due`() {
        // The phrase "payment of Rs" appears here, but so does "spent" — a purchase wins.
        assertFalse(CreditCardBillDetector.isBillPayment(
            "Rs.500 spent on your credit card. Minimum payment of Rs.2399 due on 15-Sep."))
    }

    @Test fun `a card transaction alert`() {
        assertFalse(CreditCardBillDetector.isBillPayment(
            "Transaction of Rs.2399.00 on your credit card ending 1234 at AMAZON."))
    }

    // ── Words that merely look like the trigger ───────────────────────────────

    @Test fun `credited is not CRED`() {
        assertFalse(CreditCardBillDetector.isBillPayment(
            "A/c XX5678 Credited for Rs:2399.00 by UPI."))
    }

    @Test fun `a salary credit is not a bill payment`() {
        assertFalse(CreditCardBillDetector.isBillPayment(
            "Rs.50000.00 credited to A/c XX5678 as SALARY."))
    }

    @Test fun `a payment with no card mentioned anywhere`() {
        assertFalse(CreditCardBillDetector.isBillPayment(
            "Payment received of Rs.2399.00 towards your home loan."))
    }

    @Test fun `blank and null are not bill payments`() {
        assertFalse(CreditCardBillDetector.isBillPayment(null))
        assertFalse(CreditCardBillDetector.isBillPayment("   "))
    }
}
