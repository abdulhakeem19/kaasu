package com.kaasu.app.notification.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountNotificationParserTest {

    private fun extract(text: String) = AccountNotificationParser.extractLastFour(text)

    // ── A/c *XXXX patterns ────────────────────────────────────────────────────

    @Test fun unionBank_acStar() =
        assertEquals("0000", extract("A/c *0000 Credited for Rs:1.00 on 11-06-2026"))

    @Test fun hdfc_acDoubleX() =
        assertEquals("1234", extract("Your A/c XX1234 has been debited for Rs.500"))

    @Test fun sbi_acManyX() =
        assertEquals("5678", extract("SBI A/c XXXXXXXX5678 is debited by Rs.1000"))

    @Test fun icici_acNoPattern() =
        assertEquals("9012", extract("Rs.500.00 debited from your ICICI Bank A/c no. XX9012"))

    // ── account ending / account XXXX patterns ────────────────────────────────

    @Test fun axis_accountEnding() =
        assertEquals("7890", extract("INR 1000 deducted from account ending 7890"))

    @Test fun generic_accountXxxx() =
        assertEquals("3456", extract("amount debited from account XXXX3456"))

    // ── Card patterns ─────────────────────────────────────────────────────────

    @Test fun creditCard_ending() =
        assertEquals("4321", extract("HDFC Bank Credit Card ending 4321 has been used"))

    @Test fun debitCard_xxxx() =
        assertEquals("2222", extract("Debit Card XX2222 used at merchant"))

    @Test fun sbiCard_cardOnly() =
        assertEquals("1111", extract("transaction on your SBI Card XX1111 for Rs.250"))

    // ── Generic masking fallback ───────────────────────────────────────────────

    @Test fun generic_xxxx_fallback() =
        assertEquals("8765", extract("charged ****8765"))

    // ── Should NOT match ──────────────────────────────────────────────────────

    @Test fun no_match_plain_amount() =
        assertNull(extract("₹5000 paid to Swiggy via Google Pay"))

    @Test fun no_match_date() =
        assertNull(extract("Transaction on 26-04-2026 via UPI"))

    @Test fun no_match_otp() =
        assertNull(extract("Your OTP is 8723. Do not share."))

    // ── Issuer + card-type detection (for auto-created account names) ──────────

    @Test fun issuer_hdfc() =
        assertEquals("HDFC", AccountNotificationParser.extractIssuer("A/c XX1234 debited at HDFC Bank"))

    @Test fun issuer_union_bank() =
        assertEquals("Union Bank", AccountNotificationParser.extractIssuer("Your SB A/c *0000 Credited - Union Bank"))

    @Test fun issuer_sbi_from_state_bank() =
        assertEquals("SBI", AccountNotificationParser.extractIssuer("State Bank of India: A/c XX5678 debited"))

    @Test fun issuer_none() =
        assertNull(AccountNotificationParser.extractIssuer("₹500 paid to Swiggy"))

    @Test fun credit_card_detected() =
        assertEquals(true, AccountNotificationParser.isCreditCard("Rs.641 spent on your SBI Credit Card ending 1493"))

    @Test fun not_credit_card_for_savings() =
        assertEquals(false, AccountNotificationParser.isCreditCard("A/c XX1234 debited by Rs.500"))
}
