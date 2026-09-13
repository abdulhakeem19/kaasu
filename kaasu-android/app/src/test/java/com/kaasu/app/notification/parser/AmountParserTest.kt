package com.kaasu.app.notification.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountParserTest {

    private fun parse(text: String) = AmountParser.parse(text)

    // ── Rupee symbol prefix ──────────────────────────────────────────────────

    @Test fun rupeeSymbol_integer() = assertEquals(50000L, parse("₹500"))

    @Test fun rupeeSymbol_decimal() = assertEquals(50050L, parse("₹500.50"))

    @Test fun rupeeSymbol_thousands() = assertEquals(150000L, parse("₹1,500"))

    @Test fun rupeeSymbol_thousands_decimal() = assertEquals(150050L, parse("₹1,500.50"))

    @Test fun rupeeSymbol_lakhs() = assertEquals(10000000L, parse("₹1,00,000"))

    @Test fun rupeeSymbol_space_before_amount() = assertEquals(25000L, parse("₹ 250"))

    @Test fun rupeeSymbol_in_sentence() =
        assertEquals(35000L, parse("You paid ₹350 to Zomato via Google Pay"))

    // ── Rs. prefix ──────────────────────────────────────────────────────────

    @Test fun rs_dot_integer() = assertEquals(50000L, parse("Rs.500"))

    @Test fun rs_dot_thousands() = assertEquals(150000L, parse("Rs.1,500"))

    @Test fun rs_space_integer() = assertEquals(50000L, parse("Rs 500"))

    @Test fun rs_no_separator() = assertEquals(50000L, parse("Rs500"))

    @Test fun rs_in_bank_message() =
        assertEquals(500000L, parse("A/c XX1234 debited by Rs.5000. Avbl Bal: Rs.12,345.67"))

    // ── INR prefix ──────────────────────────────────────────────────────────

    @Test fun inr_space_integer() = assertEquals(250000L, parse("INR 2500"))

    @Test fun inr_no_space() = assertEquals(50000L, parse("INR500"))

    @Test fun inr_decimal() = assertEquals(250000L, parse("INR 2500.00"))

    // ── INR suffix ──────────────────────────────────────────────────────────

    @Test fun inr_suffix() = assertEquals(50000L, parse("500 INR"))

    @Test fun inr_suffix_decimal() = assertEquals(50050L, parse("500.50 INR"))

    // ── Edge cases ───────────────────────────────────────────────────────────

    @Test fun zero_amount_returns_null() = assertNull(parse("₹0"))

    @Test fun no_currency_no_amount_returns_null() = assertNull(parse("Transaction successful"))

    @Test fun plain_number_without_currency_returns_null() = assertNull(parse("500 paid"))

    @Test fun empty_text_returns_null() = assertNull(parse(""))

    @Test fun small_paisa_amount() = assertEquals(100L, parse("₹1"))

    @Test fun large_amount() = assertEquals(10000000000L, parse("₹10,00,00,000"))

    @Test fun rounding_half_up() = assertEquals(101L, parse("₹1.015"))

    // ── Rs: format (Union Bank) ──────────────────────────────────────────────

    @Test fun rs_colon_integer() = assertEquals(100L, parse("Rs:1.00"))

    @Test fun rs_colon_thousands() = assertEquals(94604L, parse("Rs:946.04"))

    @Test fun rs_colon_in_sentence() =
        assertEquals(100L, parse("Your SB A/c *0000 Credited for Rs:1.00 on 06-06-25"))
}
