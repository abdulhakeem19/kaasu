package com.kaasu.app.notification.parser

import com.kaasu.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MerchantParserTest {

    private fun parse(text: String, type: TransactionType = TransactionType.EXPENSE) =
        MerchantParser.parse(text, type)

    @Test fun paid_to_pattern() = assertEquals("Swiggy", parse("₹500 paid to Swiggy via Google Pay"))

    @Test fun sent_to_pattern() = assertEquals("merchant", parse("₹250 Sent to merchant@upi"))

    @Test fun transferred_to_pattern() =
        assertEquals("HDFC Credit Card", parse("Payment transferred to HDFC Credit Card"))

    @Test fun at_pattern() = assertEquals("McDonald's", parse("₹350 spent at McDonald's"))

    @Test fun from_pattern_income() =
        assertEquals("Priya Kumar", parse("₹2,500 received from Priya Kumar", TransactionType.INCOME))

    @Test fun vpa_expense() = assertEquals("swiggy", parse("₹500 paid to swiggy@icici"))

    @Test fun vpa_income() =
        assertEquals("friend", parse("₹1,000 received from friend@ybl", TransactionType.INCOME))

    @Test fun no_merchant_returns_null() =
        assertNull(parse("A/c XX1234 debited by Rs.5000"))

    @Test fun merchant_trimmed_of_punctuation() =
        assertEquals("Zomato", parse("₹199 paid to Zomato."))

    @Test fun merchant_max_50_chars() {
        val longName = "A".repeat(60)
        val result = parse("₹100 paid to $longName via UPI")
        assert((result?.length ?: 0) <= 50)
    }

    // ── AT_PATTERN stops before trailing date/time context ────────────────────

    @Test fun at_stops_before_on_date() =
        assertEquals("Swiggy", parse("INR 254.00 spent at Swiggy on 01 FEB 2026 at 02:19 PM"))

    @Test fun at_multiword_stops_before_on() =
        assertEquals("ZOMATO LIMITED", parse("INR 286.15 spent at ZOMATO LIMITED on 03 JAN 2026"))

    @Test fun at_long_single_word_stops_before_on() =
        assertEquals("KPNFARMFRESHOFFLINE", parse("INR 564.42 spent at KPNFARMFRESHOFFLINE on 03 JUN 2026 at 07:29 PM"))

    @Test fun at_stops_before_via() =
        assertEquals("Zepto", parse("Rs.641.00 spent on your SBI Credit Card at Zepto on 24-04-26 via UPI"))

    @Test fun at_end_of_string_no_on() =
        assertEquals("McDonald's", parse("₹350 spent at McDonald's"))

    // ── PAID_YOU_PATTERN: sender name extraction from GPay income format ──────

    @Test fun paid_you_single_name() =
        assertEquals("MEENAKSHI", parse("MEENAKSHI . paid you ₹1.00 for lunch", TransactionType.INCOME))

    @Test fun paid_you_multiword_name() =
        assertEquals("Priya Kumar", parse("Priya Kumar paid you ₹500", TransactionType.INCOME))

    @Test fun sent_you_single_name() =
        assertEquals("Rahul", parse("Rahul sent you ₹200", TransactionType.INCOME))

    // For EXPENSE "paid to X", TO_PATTERN returns X (the merchant), not the sender name
    @Test fun paid_to_expense_returns_destination_not_sender() =
        assertEquals("Swiggy", parse("MEENAKSHI paid to Swiggy via Google Pay", TransactionType.EXPENSE))

    // ── Junk-absorption regressions (the bug this change fixes) ───────────────

    @Test fun paid_to_stops_before_for() =
        assertEquals("Swiggy", parse("₹500 paid to Swiggy for groceries"))

    @Test fun paid_to_stops_before_digits() =
        assertEquals("Swiggy", parse("₹500 paid to Swiggy 5% offer"))

    @Test fun paid_to_stops_before_with() =
        assertEquals("Zomato", parse("₹500 paid to Zomato with discount applied"))

    @Test fun paid_to_your_account_is_null() =
        assertNull(parse("₹500 paid to your account"))

    @Test fun from_stops_before_ref() =
        assertEquals("Priya", parse("₹500 received from Priya ref 998877", TransactionType.INCOME))
}
