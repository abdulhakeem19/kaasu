package com.kaasu.app.accessibility.extraction

import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.notification.filter.PromotionalDetector
import com.kaasu.app.notification.parser.AmountParser
import com.kaasu.app.notification.parser.TransactionTypeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Google Pay's history rows arrive as one `contentDescription` per row, newline-separated:
 * `"MERCHANT\n₹20 debited\n13 September"`. Shapes below are taken from a real device dump; the
 * merchant names are invented, the structure is verbatim.
 *
 * This is the format the channel silently failed on — it read `text` (of which GPay exposes none)
 * and matched view ids (of which GPay exposes one).
 */
class GPayRowFormatTest {

    private fun row(packed: String) = TransactionRowTextBuilder.build(packed.split('\n'))

    @Test
    fun `debit row yields merchant, amount and date`() {
        val r = row("ARUN STORES 70 FEET RD\n₹20 debited\n13 September")!!
        assertEquals("₹20", r.amountText)
        assertEquals("ARUN STORES 70 FEET RD", r.merchantText)
        assertEquals("13 September", r.dateText)
        assertEquals("debited", r.directionHint)
    }

    @Test
    fun `the row's own words drive the existing parsers correctly`() {
        // The row is fed to TransactionParser verbatim. It is not rewritten into a "Paid to X"
        // sentence: the merchant travels separately as an override, and that prefix made every
        // promotional row look like a receipt to PromotionalDetector.
        val sentence = row("KOORAI KADAI BIRYANI\n₹160 debited\n13 September")!!.sentence
        assertEquals(16000L, AmountParser.parse(sentence))
        assertEquals(TransactionType.EXPENSE, TransactionTypeParser.parse(sentence))
    }

    @Test
    fun `an inbound row parses as income rather than expense`() {
        val sentence = row("MEENA R\n₹1,200 credited\n12 September")!!.sentence
        assertEquals(TransactionType.INCOME, TransactionTypeParser.parse(sentence))
        assertEquals(120000L, AmountParser.parse(sentence))
    }

    @Test
    fun `a promotional row is recognised as promotional, not as a payment`() {
        // Regression: GPay lists offers in the same shape. This was captured as a ₹40 expense while
        // the row was being rewritten with a "Paid to" prefix, which satisfied the detector's
        // "a real receipt says paid to" escape hatch.
        val sentence = row("Personal loan\nUp to ₹40 lakh, instant approval\nCheck details")!!.sentence
        assertTrue(PromotionalDetector.isPromotional(sentence))
    }

    @Test
    fun `raw sentence is preserved alongside the canonical one`() {
        val r = row("ARUN STORES 70 FEET RD\n₹20 debited\n13 September")!!
        assertEquals("ARUN STORES 70 FEET RD ₹20 debited 13 September", r.sentence)
    }

    @Test
    fun `a promotional row on the same screen carries no direction and is dropped upstream`() {
        // GPay renders offers in the same list shape. This one has an amount but no direction verb,
        // and PromotionalDetector catches it downstream on "up to".
        val r = row("Personal loan\nUp to ₹40 lakh, instant approval\nCheck details")
        assertNotNull(r)
        assertNull(r!!.directionHint)
    }

    @Test
    fun `a row with no amount is not a transaction`() {
        assertNull(row("Bank balances\n5 accounts\nCheck"))
    }
}
