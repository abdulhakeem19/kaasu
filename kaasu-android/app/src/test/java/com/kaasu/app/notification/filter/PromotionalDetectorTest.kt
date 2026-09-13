package com.kaasu.app.notification.filter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromotionalDetectorTest {

    // ── Offers / promotions must be flagged as promotional ────────────────────

    @Test fun cashback_offer_with_code() = assertTrue(
        PromotionalDetector.isPromotional("Get ₹100 cashback on your next Swiggy order! Use code SAVE100")
    )

    @Test fun flat_discount_shop_now() = assertTrue(
        PromotionalDetector.isPromotional("Flat 50% off on Myntra. Shop now!")
    )

    @Test fun preapproved_loan_apply_now() = assertTrue(
        PromotionalDetector.isPromotional("You are eligible for a pre-approved loan of Rs 5,00,000. Apply now")
    )

    @Test fun win_cashback_limited_period() = assertTrue(
        PromotionalDetector.isPromotional("Win ₹1000 cashback! Limited period offer on PhonePe")
    )

    @Test fun upto_off_book_now() = assertTrue(
        PromotionalDetector.isPromotional("Upto 40% off on flights. Book now with MakeMyTrip")
    )

    @Test fun earn_reward_points() = assertTrue(
        PromotionalDetector.isPromotional("Earn 5X reward points on your HDFC card this weekend")
    )

    @Test fun voucher_promo() = assertTrue(
        PromotionalDetector.isPromotional("Your Amazon voucher worth ₹500 is waiting. Claim now!")
    )

    // ── Real receipts must NOT be flagged (even when they mention an offer) ────

    @Test fun real_paid_to_merchant() = assertFalse(
        PromotionalDetector.isPromotional("₹500.00 paid to Swiggy via Google Pay")
    )

    @Test fun real_bank_debit() = assertFalse(
        PromotionalDetector.isPromotional("A/c XX1234 debited by Rs.5000. Avbl Bal: Rs.12,345.67")
    )

    @Test fun real_credit_no_marker() = assertFalse(
        PromotionalDetector.isPromotional("INR 2500.00 credited to your A/c XX5678")
    )

    // Escape hatch: a genuine cashback receipt mentions "cashback offer" but also carries a
    // transactional signal (credited + A/c), so it must be kept.
    @Test fun genuine_cashback_receipt_with_offer_word() = assertFalse(
        PromotionalDetector.isPromotional("Cashback offer redeemed: ₹10 credited to A/c XX1234")
    )

    @Test fun real_spent_with_card_ending() = assertFalse(
        PromotionalDetector.isPromotional("Rs.641.00 spent on your SBI Credit Card ending 1493 at Zepto")
    )

    // A genuine wallet/account credit must be kept even with no card tail
    @Test fun real_money_added_to_wallet() = assertFalse(
        PromotionalDetector.isPromotional("₹100 added to your Paytm Wallet")
    )

    // ── Real-world marketing notifications (from device) that must be flagged ──

    @Test fun marketing_tap_to_get() = assertTrue(
        PromotionalDetector.isPromotional("Abdul, pick healthy today. Tap to get Lady Finger (250 gm) @ ₹15")
    )

    @Test fun marketing_stock_up_instamart() = assertTrue(
        PromotionalDetector.isPromotional("One price: ₹1 Stock up on green cucumbers, organic cucumbers & muskmelon now. Instamart")
    )

    @Test fun marketing_wallet_cashback() = assertTrue(
        PromotionalDetector.isPromotional("New to wallet? Here's ₹10 for you! Add ₹250 & get instant ₹10 cashback Pay Amazon se")
    )

    @Test fun marketing_flat_off_dinner() = assertTrue(
        PromotionalDetector.isPromotional("Give dad the remote. And FLAT ₹200 OFF on dinner too.")
    )
}
