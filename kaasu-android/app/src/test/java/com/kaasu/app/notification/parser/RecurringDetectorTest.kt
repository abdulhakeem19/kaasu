package com.kaasu.app.notification.parser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringDetectorTest {

    @Test fun autopay_is_recurring() =
        assertTrue(RecurringDetector.isRecurring("Rs.649 debited via autopay for Netflix"))

    @Test fun emandate_execution_is_recurring() =
        assertTrue(RecurringDetector.isRecurring("INR 199 debited as per e-mandate for Spotify"))

    @Test fun sip_is_recurring() =
        assertTrue(RecurringDetector.isRecurring("Rs.5000 debited for SIP - Axis Mutual Fund"))

    @Test fun emi_is_recurring() =
        assertTrue(RecurringDetector.isRecurring("Your EMI of Rs.2,499 has been debited"))

    @Test fun normal_payment_not_recurring() =
        assertFalse(RecurringDetector.isRecurring("₹500 paid to Swiggy via Google Pay"))

    @Test fun normal_bank_debit_not_recurring() =
        assertFalse(RecurringDetector.isRecurring("A/c XX1234 debited by Rs.5000"))
}
