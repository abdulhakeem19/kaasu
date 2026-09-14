package com.kaasu.app.accessibility.extraction

import com.kaasu.app.notification.filter.PendingPaymentDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Google Pay's per-contact payment rows, taken verbatim from rows this app actually stored wrongly
 * on a real device. Contact names are invented; the structure is exact.
 */
class GPayPaymentRowTest {

    private fun row(text: String) = TransactionRowTextBuilder.build(listOf(text))

    @Test
    fun `the typed note is separated from the contact name`() {
        val r = row("Payment to Saraswathy gas ₹1,000 Paid • 1 Sept")!!
        assertEquals("Saraswathy", r.merchantText)
        assertEquals("gas", r.noteText)
    }

    @Test
    fun `a multi-word note stays whole`() {
        val r = row("Payment to Saraswathy trip balance amount ₹5,000 Paid • 9 Sept")!!
        assertEquals("Saraswathy", r.merchantText)
        assertEquals("trip balance amount", r.noteText)
    }

    @Test
    fun `a payment with no note keeps the whole name as the merchant`() {
        val r = row("Payment to Saraswathy ₹1 Paid • 10:31 am")!!
        assertEquals("Saraswathy", r.merchantText)
        assertEquals(null, r.noteText)
    }

    @Test
    fun `the date is read from the bullet separated tail`() {
        // The bullet used to stay joined to the status, so the date match covered less than half
        // its fragment and was thrown away — the row then fell back to today, which is how
        // payments from the 1st and 5th were stored as if they happened today.
        val r = row("Payment to Saraswathy gas ₹1,000 Paid • 1 Sept")!!
        assertEquals("1 Sept", r.dateText)
    }

    @Test
    fun `a failed payment is never treated as money leaving the account`() {
        val text = "Payment to Saraswathy chumma ₹10 Failed • 10:28 am " +
            "Your money was not debited Receiver's payment server was busy."
        assertTrue(PendingPaymentDetector.isPending(text))
    }

    @Test
    fun `a successful payment is not mistaken for a failed one`() {
        assertTrue(!PendingPaymentDetector.isPending("Payment to Saraswathy gas ₹1,000 Paid • 1 Sept"))
    }
}
