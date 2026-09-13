package com.kaasu.app.statement.parser.xlsx

import com.kaasu.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Tests parser logic directly against a hand-written pipe-delimited string, matching the exact
 * shape [XlsxTextExtractor] produces (see that class's own test for extraction-step coverage).
 * Fabricated names/amounts/IDs, but the header row and every Particulars-prefix case below is
 * structurally identical to the verified real IDFC FIRST Bank sample.
 */
class IdfcFirstSavingsXlsxParserTest {

    // Each row is exactly 7 pipe-delimited fields (Date|ValueDate|Particulars|ChequeNo|Debit|
    // Credit|Balance), matching what XlsxTextExtractor produces — see that class's own test.
    private val sampleText = """
        STATEMENT OF ACCOUNT
        CUSTOMER ID|9999999999
        ACCOUNT NUMBER|10100000000
        Transaction Date|Value Date|Particulars|Cheque No.|Debit|Credit|Balance
        02-May-2026|02-May-2026|UPI/DR/900000000001/RAMESH K/CNRB/ramesh/lunch||175.0||830.49
        09-May-2026|09-May-2026|UPI/CR/900000000002/SUNITA R/UBIN/sunita/rent|||14000.0|15192.49
        04-May-2026|04-May-2026|BLKIFT/OnscreenPayment/Salary|||57120.0|57720.49
        04-May-2026|04-May-2026|IFT-OPT/IFT/20261243117077/040526/1374||16327.0||12266.49
        14-May-2026|14-May-2026|POS-VISA/NETFLIX/900000000003/CHENNAI/18:46:11||149.0||5332.49
        31-May-2026|31-May-2026|MONTHLY INTEREST CREDIT|||8.0|80.66
        |||||||
        |Total|||211192.79|210550.0|
        |Total number of Debits|124.0|||||
        |Total number of Credits|14.0|||||
        ||End of the Statement||||
    """.trimIndent()

    @Test fun canParse_matchesHeader() {
        assertTrue(IdfcFirstSavingsXlsxParser.canParse(sampleText))
    }

    @Test fun canParse_rejectsUnrelatedText() {
        assertFalse(IdfcFirstSavingsXlsxParser.canParse("Txn Date,Description,Amount,Type"))
    }

    @Test fun parse_extractsUpiDebitWithTransactionId() {
        val items = IdfcFirstSavingsXlsxParser.parse(sampleText)
        val debit = items.first { it.direction == TransactionType.EXPENSE && it.upiTransactionId == "900000000001" }
        assertEquals(LocalDate.of(2026, 5, 2), debit.date)
        assertEquals(17500L, debit.amountInPaise)
        assertEquals("RAMESH K", debit.description)
    }

    @Test fun parse_extractsUpiCreditWithTransactionId() {
        val items = IdfcFirstSavingsXlsxParser.parse(sampleText)
        val credit = items.first { it.direction == TransactionType.INCOME && it.upiTransactionId == "900000000002" }
        assertEquals(LocalDate.of(2026, 5, 9), credit.date)
        assertEquals(1400000L, credit.amountInPaise)
        assertEquals("SUNITA R", credit.description)
    }

    @Test fun parse_blkiftMapsToSalaryWithNoUpiId() {
        val items = IdfcFirstSavingsXlsxParser.parse(sampleText)
        val salary = items.first { it.description == "Salary" }
        assertEquals(TransactionType.INCOME, salary.direction)
        assertEquals(5712000L, salary.amountInPaise)
        assertNull(salary.upiTransactionId)
    }

    @Test fun parse_iftOptMapsToFundTransfer() {
        val items = IdfcFirstSavingsXlsxParser.parse(sampleText)
        val transfer = items.first { it.description == "Fund Transfer" }
        assertEquals(TransactionType.EXPENSE, transfer.direction)
        assertEquals(1632700L, transfer.amountInPaise)
        assertNull(transfer.upiTransactionId)
    }

    @Test fun parse_posVisaUsesMerchantSegment() {
        val items = IdfcFirstSavingsXlsxParser.parse(sampleText)
        val pos = items.first { it.description == "NETFLIX" }
        assertEquals(TransactionType.EXPENSE, pos.direction)
        assertEquals(14900L, pos.amountInPaise)
    }

    @Test fun parse_monthlyInterestMapsToInterest() {
        val items = IdfcFirstSavingsXlsxParser.parse(sampleText)
        val interest = items.first { it.description == "Interest" }
        assertEquals(TransactionType.INCOME, interest.direction)
        assertEquals(800L, interest.amountInPaise)
    }

    @Test fun parse_skipsFooterAndSummaryRows() {
        // 6 real transaction rows above; the blank row + 4 footer/summary rows must all be skipped
        // because their Transaction Date column doesn't parse as a date.
        assertEquals(6, IdfcFirstSavingsXlsxParser.parse(sampleText).size)
    }

    @Test fun parse_returnsEmptyWhenNoHeaderFound() {
        assertTrue(IdfcFirstSavingsXlsxParser.parse("not a statement at all").isEmpty())
    }
}
