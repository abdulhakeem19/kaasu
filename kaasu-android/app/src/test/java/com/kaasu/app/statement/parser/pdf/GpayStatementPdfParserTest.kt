package com.kaasu.app.statement.parser.pdf

import com.kaasu.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Fabricated names/amounts/IDs, but the block structure below (line order, "Paid to"/"Received
 * from"/"Self transfer to" verbs, the settlement line, the page-header "Amount" text leaking in)
 * is structurally identical to the verified real Google Pay statement extraction — see
 * [GpayStatementPdfParser]'s doc comment for exactly what was verified and how.
 */
class GpayStatementPdfParserTest {

    private val extractedText = """
        Transaction statement
        9999999999, test@example.com
        Note: This statement reflects payments made by you on the Google Pay app. Self transfer payments are not included in the total money paid and
        received. Any payments transactions and activity deleted from your Google Account will not show up in this statement.
        Page 1 of 1
        Transaction statement period
        01 May 2026 - 31 May 2026
        Sent
        ₹500
        Received
        ₹1,000
        Date & time Transaction details Amount
        01 May, 2026
        10:47 AM
        Paid to Kumar Traders
        UPI Transaction ID: 700000000001
        Paid by State Bank of India XX93 | RuPay credit card
        ₹111
        06 May, 2026
        07:28 PM
        Received from Priya Nair
        UPI Transaction ID: 700000000002
        Paid to Union Bank of India 0913
        ₹2,500
        09 May, 2026
        12:07 PM
        Self transfer to IDFC FIRST Bank 3956
        UPI Transaction ID: 700000000003
        Paid by Union Bank of India 0913
        ₹14,000
        10 May, 2026
        08:15 PM
        Paid to Big Bazaar
        UPI Transaction ID: 700000000004
        Paid by IDFC FIRST Bank 3956
        ₹11,500
        Page 1 of 1
    """.trimIndent()

    @Test fun canParse_matchesGooglePayHeader() {
        assertTrue(GpayStatementPdfParser.canParse(extractedText))
    }

    @Test fun canParse_rejectsUnrelatedText() {
        assertFalse(GpayStatementPdfParser.canParse("HDFC BANK LIMITED Statement of Account"))
    }

    @Test fun parse_extractsPaidToAsExpenseWithUpiId() {
        val items = GpayStatementPdfParser.parse(extractedText)
        val expense = items.first { it.upiTransactionId == "700000000001" }
        assertEquals(TransactionType.EXPENSE, expense.direction)
        assertEquals(LocalDate.of(2026, 5, 1), expense.date)
        assertEquals(11100L, expense.amountInPaise)
        assertEquals("Kumar Traders", expense.description)
    }

    @Test fun parse_extractsReceivedFromAsIncomeWithUpiId() {
        val items = GpayStatementPdfParser.parse(extractedText)
        val income = items.first { it.upiTransactionId == "700000000002" }
        assertEquals(TransactionType.INCOME, income.direction)
        assertEquals(LocalDate.of(2026, 5, 6), income.date)
        assertEquals(250000L, income.amountInPaise)
        assertEquals("Priya Nair", income.description)
    }

    @Test fun parse_extractsLargeAmountWithIndianCommaGrouping() {
        val items = GpayStatementPdfParser.parse(extractedText)
        val item = items.first { it.upiTransactionId == "700000000004" }
        assertEquals(1150000L, item.amountInPaise)
        assertEquals("Big Bazaar", item.description)
    }

    @Test fun parse_skipsSelfTransferBlocks() {
        val items = GpayStatementPdfParser.parse(extractedText)
        assertTrue(items.none { it.upiTransactionId == "700000000003" })
    }

    @Test fun parse_ignoresPageHeaderAndSummaryNoise() {
        // 4 real transaction blocks; the page-header "Amount" line, the Sent/Received summary
        // amounts, and the self-transfer block must not produce spurious extra items.
        assertEquals(3, GpayStatementPdfParser.parse(extractedText).size)
    }

    @Test fun parse_returnsEmptyForUnrelatedText() {
        assertTrue(GpayStatementPdfParser.parse("not a statement at all").isEmpty())
    }
}
