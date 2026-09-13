package com.kaasu.app.core.bank

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Identifiers here are real ones taken from captured transactions on a live device. */
class BankRegistryTest {

    @Test
    fun `resolves a bank from its DLT sms sender header`() {
        assertEquals("IDFC FIRST", BankRegistry.resolve("sms:JM-IDFCFB-S")?.displayName)
        assertEquals("Union Bank", BankRegistry.resolve("sms:JX-UNIONB-T")?.displayName)
        assertEquals("SBI", BankRegistry.resolve("sms:VA-SBICRD-S")?.displayName)
    }

    @Test
    fun `resolves a payment app from its package name`() {
        assertEquals("Google Pay", BankRegistry.resolve("com.google.android.apps.nbu.paisa.user")?.displayName)
        assertEquals("PhonePe", BankRegistry.resolve("com.phonepe.app")?.displayName)
        assertEquals("Paytm", BankRegistry.resolve("net.one97.paytm")?.displayName)
    }

    @Test
    fun `resolves a bank from an account display name`() {
        assertEquals("IDFC FIRST", BankRegistry.resolve("IDFC FIRST Card")?.displayName)
        assertEquals("SBI", BankRegistry.resolve("SBI Card")?.displayName)
    }

    @Test
    fun `the same bank resolves to one identity regardless of which identifier is used`() {
        val fromSms = BankRegistry.resolve("sms:AD-IDFCFB-S")
        val fromName = BankRegistry.resolve("IDFC FIRST")
        val fromPackage = BankRegistry.resolve("com.idfcfirstbank.optimus")
        assertEquals(fromSms, fromName)
        assertEquals(fromSms, fromPackage)
    }

    @Test
    fun `falls through the identifiers in order until one matches`() {
        assertEquals("SBI", BankRegistry.resolve(null, "", "sms:JD-SBICRD-S")?.displayName)
    }

    @Test
    fun `an unrecognised identifier resolves to null rather than a wrong bank`() {
        assertNull(BankRegistry.resolve("sms:VA-ZEPTON-S"))
        assertNull(BankRegistry.resolve("Cash Wallet"))
    }

    @Test
    fun `unknown accounts fall back to their own initials`() {
        assertEquals("CW", BankRegistry.monogramFor("Cash Wallet"))
        assertEquals("B", BankRegistry.monogramFor("Bank"))
        assertEquals("•", BankRegistry.monogramFor("   "))
    }
}
