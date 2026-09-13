package com.kaasu.app.statement.parser.pdf

import com.kaasu.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HdfcSavingsPdfParserTest {

    // Simulates PdfBox-extracted text: table alignment is gone, one transaction per line with
    // its cells collapsed to single spaces (see the ASSUMED FORMAT comment on the parser).
    private val extractedText = """
        HDFC BANK LIMITED
        Statement of Account for account number 401234567890

        Date          Narration                                    Chq/Ref No.  Value Dt  Withdrawal Amt.  Deposit Amt.  Closing Balance

        01/04/24 UPI-SWIGGY-swiggy@ybl-401234567890-Payment Dr 150.00 45,320.00
        02/04/24 NEFT-SALARY-ACME CORP Cr 50,000.00 95,320.00
        this line has no date or Dr/Cr amount shape at all
    """.trimIndent()

    @Test fun canParse_matchesHdfcStatementHeader() {
        assertTrue(HdfcSavingsPdfParser.canParse(extractedText))
    }

    @Test fun canParse_rejectsOtherBankText() {
        assertFalse(HdfcSavingsPdfParser.canParse("SBI Card Statement Transaction Date,Description,Amount,Type"))
    }

    @Test fun parse_extractsDebitLineAsExpense() {
        val items = HdfcSavingsPdfParser.parse(extractedText)
        val expense = items.first { it.direction == TransactionType.EXPENSE }
        assertEquals(LocalDate.of(2024, 4, 1), expense.date)
        assertEquals(15000L, expense.amountInPaise)
        assertEquals("UPI-SWIGGY-swiggy@ybl-401234567890-Payment", expense.description)
    }

    @Test fun parse_extractsCreditLineAsIncome() {
        val items = HdfcSavingsPdfParser.parse(extractedText)
        val income = items.first { it.direction == TransactionType.INCOME }
        assertEquals(LocalDate.of(2024, 4, 2), income.date)
        assertEquals(5000000L, income.amountInPaise)
    }

    @Test fun parse_skipsNonTransactionLines() {
        assertEquals(2, HdfcSavingsPdfParser.parse(extractedText).size)
    }
}
