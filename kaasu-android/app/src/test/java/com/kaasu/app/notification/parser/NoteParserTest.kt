package com.kaasu.app.notification.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteParserTest {

    @Test
    fun `gpay note typed by the payer is captured`() {
        assertEquals("Bike repair", NoteParser.parse("RAVI KUMAR S paid you ₹300.00 Bike repair"))
    }

    @Test
    fun `thousands separator does not leak into the note`() {
        assertEquals("Movie tickets", NoteParser.parse("RAVI KUMAR S paid you ₹5,000.00 Movie tickets"))
    }

    @Test
    fun `single word note is captured`() {
        assertEquals("for lunch", NoteParser.parse("MEENAKSHI . paid you ₹15,000.00 for lunch"))
    }

    @Test
    fun `app boilerplate is not a note`() {
        assertNull(NoteParser.parse("ANAND RAJ M paid you ₹500.00 Tap to view."))
    }

    @Test
    fun `channel hint is not a note`() {
        assertNull(NoteParser.parse("FAROOQ AHMED A paid you ₹3,149.00 Sent using Paytm UPI"))
    }

    @Test
    fun `boilerplate trailing a real note is trimmed off`() {
        assertEquals("Bike repair", NoteParser.parse("RAVI KUMAR S paid you ₹300.00 Bike repair Tap to view."))
    }

    @Test
    fun `no trailing text yields null`() {
        assertNull(NoteParser.parse("Paid to Swiggy ₹245"))
    }

    @Test
    fun `bank receipt tail is not treated as a note`() {
        assertNull(
            NoteParser.parse("Union Bank of India A/c *0000 Debited Rs:115.00 Avl Bal Rs:1391.54")
        )
    }
}
