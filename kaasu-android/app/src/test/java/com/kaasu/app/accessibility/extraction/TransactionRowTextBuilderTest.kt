package com.kaasu.app.accessibility.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionRowTextBuilderTest {

    @Test fun reconstructsGPayStyleRow() {
        val row = TransactionRowTextBuilder.build(listOf("Paid to Swiggy", "₹245", "28 Aug"))!!
        assertEquals("Paid to Swiggy ₹245 28 Aug", row.sentence)
        assertEquals("₹245", row.amountText)
        assertEquals("Swiggy", row.merchantText)
        assertEquals("28 Aug", row.dateText)
        assertEquals("paid to", row.directionHint)
    }

    @Test fun reconstructsReceivedFromRow() {
        val row = TransactionRowTextBuilder.build(listOf("Received from Priya Sharma", "₹1,200.50", "Yesterday"))!!
        assertEquals("₹1,200.50", row.amountText)
        assertEquals("Priya Sharma", row.merchantText)
        assertEquals("Yesterday", row.dateText)
        assertEquals("received from", row.directionHint)
    }

    @Test fun handlesRsPrefixAmount() {
        val row = TransactionRowTextBuilder.build(listOf("Paid to Zomato", "Rs. 599", "5 Jul 2026"))!!
        assertTrue(row.amountText!!.contains("599"))
        assertEquals("Zomato", row.merchantText)
    }

    @Test fun fallsBackToFirstNonAmountNonDateFragmentAsMerchantWhenNoVerbFound() {
        val row = TransactionRowTextBuilder.build(listOf("Electricity Board", "₹800", "1 Sep"))!!
        assertEquals("Electricity Board", row.merchantText)
    }

    @Test fun returnsNullWhenNoAmountFragmentPresent() {
        val row = TransactionRowTextBuilder.build(listOf("Paid to Swiggy", "28 Aug"))
        assertNull(row)
    }

    @Test fun returnsNullForEmptyFragments() {
        assertNull(TransactionRowTextBuilder.build(emptyList()))
        assertNull(TransactionRowTextBuilder.build(listOf("   ", "")))
    }

    @Test fun dateTextNullWhenNoDateFragmentPresent() {
        val row = TransactionRowTextBuilder.build(listOf("Paid to Swiggy", "₹245"))!!
        assertNull(row.dateText)
        assertEquals("₹245", row.amountText)
    }
}
