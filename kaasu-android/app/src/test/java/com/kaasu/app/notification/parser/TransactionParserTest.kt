package com.kaasu.app.notification.parser

import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.notification.model.RawNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransactionParserTest {

    private lateinit var parser: TransactionParser

    @Before
    fun setUp() {
        parser = TransactionParser()
    }

    private fun notification(
        pkg: String = "com.google.android.apps.nbu.paisa.user",
        title: String? = null,
        text: String? = null
    ) = RawNotification(packageName = pkg, appName = null, title = title, text = text, subText = null)

    // ── GPay notifications ───────────────────────────────────────────────────

    @Test fun gpay_paid_to_merchant() {
        val n = notification(text = "₹500.00 paid to Swiggy via Google Pay")
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(50000L, result!!.amountInPaise)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals("Swiggy", result.merchantName)
        assertTrue(result.confidenceScore >= 85)
    }

    @Test fun gpay_received_from_person() {
        val n = notification(text = "₹2,500 received from Priya Kumar")
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(250000L, result!!.amountInPaise)
        assertEquals(TransactionType.INCOME, result.type)
    }

    @Test fun gpay_debited_no_merchant() {
        val n = notification(text = "₹1,200.00 debited from HDFC Bank XX1234")
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(120000L, result!!.amountInPaise)
        assertEquals(TransactionType.EXPENSE, result.type)
    }

    // ── PhonePe notifications ────────────────────────────────────────────────

    @Test fun phonepe_sent_to_vpa() {
        val n = notification(
            pkg = "com.phonepe.app",
            text = "₹250.00 Sent to merchant@upi"
        )
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(25000L, result!!.amountInPaise)
        assertEquals(TransactionType.EXPENSE, result.type)
    }

    @Test fun phonepe_received() {
        val n = notification(
            pkg = "com.phonepe.app",
            title = "Money Received",
            text = "₹1,000 received from friend@ybl"
        )
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(TransactionType.INCOME, result!!.type)
    }

    // ── Paytm notifications ──────────────────────────────────────────────────

    @Test fun paytm_rs_prefix() {
        val n = notification(
            pkg = "net.one97.paytm",
            title = "Payment Done!",
            text = "Rs.750.50 paid to Swiggy"
        )
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(75050L, result!!.amountInPaise)
    }

    @Test fun paytm_wallet_added() {
        val n = notification(
            pkg = "net.one97.paytm",
            text = "Rs.1000 added to Paytm Wallet"
        )
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(TransactionType.INCOME, result!!.type)
    }

    // ── Bank app notifications ───────────────────────────────────────────────

    @Test fun hdfc_bank_debit() {
        val n = notification(
            pkg = "com.csam.icici.bank.imobile",
            text = "A/c XX1234 debited by Rs.5000. Avbl Bal: Rs.12,345.67"
        )
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(500000L, result!!.amountInPaise)
        assertEquals(TransactionType.EXPENSE, result.type)
    }

    @Test fun bank_inr_credited() {
        val n = notification(
            pkg = "com.sbi.lotusintouch",
            text = "INR 2500.00 credited to your A/c XX5678"
        )
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(250000L, result!!.amountInPaise)
        assertEquals(TransactionType.INCOME, result.type)
    }

    // ── BHIM ─────────────────────────────────────────────────────────────────

    @Test fun bhim_payment_success() {
        val n = notification(
            pkg = "in.org.npci.upiapp",
            text = "₹500 paid successfully to merchant@upi"
        )
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(50000L, result!!.amountInPaise)
        assertEquals(TransactionType.EXPENSE, result.type)
    }

    // ── Refund / cashback ─────────────────────────────────────────────────────

    @Test fun refund_notification() {
        val n = notification(text = "Refund of ₹199 processed for order #12345")
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(TransactionType.REFUND, result!!.type)
    }

    @Test fun cashback_notification() {
        val n = notification(text = "₹50 cashback credited to your Google Pay balance")
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(TransactionType.CASHBACK, result!!.type)
    }

    // ── Rejection cases ───────────────────────────────────────────────────────

    @Test fun no_amount_returns_null() {
        val n = notification(text = "Your payment was successful")
        assertNull(parser.parse(n))
    }

    @Test fun blank_text_returns_null() {
        val n = notification(text = null, title = null)
        assertNull(parser.parse(n))
    }

    @Test fun unknown_app_with_no_amount_returns_null() {
        val n = notification(pkg = "com.random.app", text = "Check out our new offer!")
        assertNull(parser.parse(n))
    }

    // ── Promotional rejection (offers with an amount must NOT be saved) ────────

    @Test fun promo_cashback_offer_rejected() {
        val n = notification(text = "Get ₹100 cashback on your next order! Use code SAVE100 on Swiggy")
        assertNull(parser.parse(n))
    }

    @Test fun promo_offer_from_known_app_rejected() {
        // Even a known finance app must not have its offers saved
        val n = notification(
            pkg = "com.phonepe.app",
            text = "Flat 50% off, shop now at Swiggy. Hurry, limited period!"
        )
        assertNull(parser.parse(n))
    }

    @Test fun promo_preapproved_loan_rejected() {
        val n = notification(text = "You are eligible for a pre-approved loan of ₹5,00,000. Apply now")
        assertNull(parser.parse(n))
    }

    // Wallet top-up "cashback" marketing previously leaked through as CASHBACK income
    @Test fun promo_wallet_cashback_rejected() {
        val n = notification(
            pkg = "com.amazon.mShop.android.shopping",
            text = "New to wallet? Here's ₹10 for you! Add ₹250 & get instant ₹10 cashback Pay Amazon se"
        )
        assertNull(parser.parse(n))
    }

    // A genuine cashback receipt that also says "cashback offer" but has a real signal is kept
    @Test fun genuine_cashback_receipt_kept() {
        val n = notification(text = "Cashback offer redeemed: ₹10 credited to A/c XX1234")
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(1000L, result!!.amountInPaise)
        assertEquals(TransactionType.CASHBACK, result.type)
    }

    // ── Direction required: an amount with no debit/credit verb is not a transaction ──

    @Test fun amount_without_direction_returns_null() {
        val n = notification(text = "Your order of ₹599 is confirmed and will arrive soon")
        assertNull(parser.parse(n))
    }

    // ── Confidence scoring ────────────────────────────────────────────────────

    @Test fun known_app_boosts_confidence() {
        val known = notification(
            pkg = "com.google.android.apps.nbu.paisa.user",
            text = "₹500 debited"
        )
        val unknown = notification(
            pkg = "com.unknown.bank.app",
            text = "₹500 debited"
        )
        val knownResult = parser.parse(known)
        val unknownResult = parser.parse(unknown)
        assertNotNull(knownResult)
        assertNotNull(unknownResult)
        assertTrue(knownResult!!.confidenceScore > unknownResult!!.confidenceScore)
    }

    @Test fun full_confidence_with_all_signals() {
        // amount + direction + merchant + strong evidence (UPI Ref) + known app
        val n = notification(
            pkg = "com.google.android.apps.nbu.paisa.user",
            text = "₹500 paid to Swiggy. UPI Ref 123456789012"
        )
        val result = parser.parse(n)
        assertNotNull(result)
        assertTrue(result!!.confidenceScore >= 100)
    }

    // ── Real-world: GPay "paid you" = INCOME, not EXPENSE ────────────────────

    @Test fun gpay_paidYou_isIncome() {
        val n = notification(
            pkg = "com.google.android.apps.nbu.paisa.user",
            text = "MEENAKSHI . paid you ₹1.00 for lunch"
        )
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(100L, result!!.amountInPaise)
        assertEquals(TransactionType.INCOME, result.type)
        assertEquals("MEENAKSHI", result.merchantName)
    }

    @Test fun gpay_paidYou_multiWordSender() {
        val n = notification(
            pkg = "com.google.android.apps.nbu.paisa.user",
            text = "Priya Kumar paid you ₹500.00"
        )
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(TransactionType.INCOME, result!!.type)
        assertEquals("Priya Kumar", result.merchantName)
    }

    // ── Real-world: Union Bank (Mathematical Sans-Serif Unicode + Rs: format) ──

    @Test fun unionBank_credit_mathUnicodeAndRsColon() {
        // Actual notification: "𝖸𝗈𝗎𝗋 𝖲𝖡 A/c *0000 𝖢𝗋𝖾𝖽𝗂𝗍𝖾𝖽 𝖿𝗈𝗋 Rs:1.00 on 06-06-25. Bal:Rs12,345.67"
        val n = notification(
            pkg = "com.UnionBank.retail",
            text = "𝖸𝗈𝗎𝗋 𝖲𝖡 A/c *0000 𝖢𝗋𝖾𝖽𝗂𝗍𝖾𝖽 𝖿𝗈𝗋 Rs:1.00 on 06-06-25. Bal:Rs12,345.67"
        )
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(100L, result!!.amountInPaise)
        assertEquals(TransactionType.INCOME, result.type)
    }

    // ── Real-world: SBI Credit Card (Mathematical Sans-Serif "𝗌𝗉𝖾𝗇𝗍") ─────────

    @Test fun sbiCard_spent_mathUnicode() {
        // Actual notification: "Rs.381.23 𝗌𝗉𝖾𝗇𝗍 𝗈𝗇 𝗒𝗈𝗎𝗋 𝖲𝖡𝖨 𝖢𝗋𝖾𝖽𝗂𝗍 𝖢𝖺𝗋𝖽 XX1234"
        val n = notification(
            pkg = "com.sbi.card",
            text = "Rs.381.23 𝗌𝗉𝖾𝗇𝗍 𝗈𝗇 𝗒𝗈𝗎𝗋 𝖲𝖡𝖨 𝖢𝗋𝖾𝖽𝗂𝗍 𝖢𝖺𝗋𝖽 XX1234 at Swiggy"
        )
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(38123L, result!!.amountInPaise)
        assertEquals(TransactionType.EXPENSE, result.type)
    }

    // ── Real-world: IDFC FIRST Bank ───────────────────────────────────────────

    @Test fun idfcFirst_payment_debit() {
        val n = notification(
            pkg = "com.idfcfirstbank.optimus",
            text = "Rs.1,500.00 spent on your IDFC FIRST Bank Credit Card XX5678 at Zomato"
        )
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(150000L, result!!.amountInPaise)
        assertEquals(TransactionType.EXPENSE, result.type)
    }

    // ── Real-world: Slice ──────────────────────────────────────────────────────

    @Test fun slice_debit() {
        val n = notification(
            pkg = "com.slicepay",
            text = "Rs.299.00 debited from Slice for Netflix"
        )
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(29900L, result!!.amountInPaise)
        assertEquals(TransactionType.EXPENSE, result.type)
    }

    // ── Real-world: CRED ──────────────────────────────────────────────────────

    @Test fun cred_creditCardPayment() {
        val n = notification(
            pkg = "com.dreamplug.androidapp",
            text = "₹5,000.00 paid to HDFC Credit Card via CRED"
        )
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(500000L, result!!.amountInPaise)
        assertEquals(TransactionType.EXPENSE, result.type)
    }

    // ── Real-world: IDFC FIRST Bank ("at MERCHANT on DATE" format) ───────────

    @Test fun idfcFirst_inrPrefix_atMerchantOnDate() {
        val n = notification(
            pkg = "com.idfcfirstbank.optimus",
            text = "Delicious Purchase! INR 254.00 spent on your IDFC FIRST Bank Credit Card ending XX5658 at Swiggy on 01 FEB 2026 at 02:19 PM Avbl Limit: INR 9459.23"
        )
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(25400L, result!!.amountInPaise)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals("Swiggy", result.merchantName)
    }

    @Test fun idfcFirst_multiwordMerchant() {
        val n = notification(
            pkg = "com.idfcfirstbank.optimus",
            text = "Delicious Purchase! INR 286.15 spent on your IDFC FIRST Bank Credit Card ending XX5658 at ZOMATO LIMITED on 03 JAN 2026 at 11:13 PM Avbl Limit: INR 9359.25"
        )
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(28615L, result!!.amountInPaise)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals("ZOMATO LIMITED", result.merchantName)
    }

    @Test fun idfcFirst_creditedWithSalary() {
        // Account credited — should be INCOME regardless of "Credit Card" appearing elsewhere
        val n = notification(
            pkg = "com.idfcfirstbank.optimus",
            text = "Your A/C XXXXX103956 is credited with INR 57,120.00 on 01/06/26 11:31. Your new balance is INR 57,175.66. Team IDFC FIRST Bank"
        )
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(5712000L, result!!.amountInPaise)
        assertEquals(TransactionType.INCOME, result.type)
    }

    // ── Real-world: "debited … ; RECIPIENT credited" UPI P2P format ──────────

    @Test fun idfcFirst_debitedWithRecipientCredited_isExpense() {
        // "Mohan K credited" refers to the recipient, not your account — must be EXPENSE
        val n = notification(
            pkg = "com.idfcfirstbank.optimus",
            text = "Your A/c XX1234 debited by Rs. 50.00 on 09/05/26; Mohan K credited. RRN 612981703454. Available balance Rs. 8,142.49. Team IDFC FIRST Bank"
        )
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(5000L, result!!.amountInPaise)
        assertEquals(TransactionType.EXPENSE, result.type)
    }

    // ── Real-world: SBI Card UPI transaction ─────────────────────────────────

    @Test fun sbiCard_upiSpent_merchantFromAt() {
        val n = notification(
            pkg = "com.sbi.card",
            text = "Rs.641.00 spent on your SBI Credit Card ending with 1493 at Zepto on 24-04-26 via UPI (Ref No. 648089284134). Trxn. not done by you? Report at https://sbicard.com/Dispute"
        )
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(64100L, result!!.amountInPaise)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals("Zepto", result.merchantName)
    }

    @Test fun sbiCard_mathUnicode_merchantFromAt() {
        // "𝗌𝗉𝖾𝗇𝗍 𝗈𝗇 𝗒𝗈𝗎𝗋 𝖲𝖡𝖨 𝖢𝗋𝖾𝖽𝗂𝗍 𝖢𝖺𝗋𝖽" normalises to "spent on your SBI Credit Card"
        val n = notification(
            pkg = "com.sbi.card",
            text = "Rs.814.00 𝗌𝗉𝖾𝗇𝗍 𝗈𝗇 𝗒𝗈𝗎𝗋 𝖲𝖡𝖨 𝖢𝗋𝖾𝖽𝗂𝗍 𝖢𝖺𝗋𝖽 𝖾𝗇𝖽𝗂𝗇𝗀 9400 at ZEPTOMARKETPLACEPRIV on 13/05/26"
        )
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(81400L, result!!.amountInPaise)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals("ZEPTOMARKETPLACEPRIV", result.merchantName)
    }

    // ── New packages appear in KNOWN_FINANCE_PACKAGES ────────────────────────

    @Test fun newPackages_boostedConfidence() {
        listOf(
            "com.idfcfirstbank.optimus",
            "com.sbi.card",
            "com.slicepay",
            "com.UnionBank.retail",
            "com.dreamplug.androidapp",
        ).forEach { pkg ->
            val n = notification(pkg = pkg, text = "₹500 debited")
            val result = parser.parse(n)
            assertNotNull("Expected result for $pkg", result)
            assertTrue("Expected confidence >= 70 for $pkg", result!!.confidenceScore >= 70)
        }
    }

    // ── SMS DLT bank sender header: additive-only confidence nudge ──────────

    @Test fun smsDltBankSenderHeader_boostsConfidence() {
        val dltSender = notification(pkg = "sms:HD-HDFCBK", text = "₹500 debited")
        val plainSms = notification(pkg = "sms:HDFCBANK", text = "₹500 debited")
        val dltResult = parser.parse(dltSender)
        val plainResult = parser.parse(plainSms)
        assertNotNull(dltResult)
        assertNotNull(plainResult)
        assertTrue(dltResult!!.confidenceScore > plainResult!!.confidenceScore)
    }

    @Test fun smsNonDltSender_stillParsedOnContentAlone() {
        // Sender format is only ever a soft nudge — a non-DLT-looking sender must still parse fine.
        val n = notification(pkg = "sms:HDFCBANK", text = "A/c XX1234 debited by Rs.5000. Avbl Bal: Rs.12,345.67")
        val result = parser.parse(n)
        assertNotNull(result)
        assertEquals(500000L, result!!.amountInPaise)
    }
}
