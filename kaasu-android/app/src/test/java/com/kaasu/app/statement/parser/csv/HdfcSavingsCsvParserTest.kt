package com.kaasu.app.statement.parser.csv

import com.kaasu.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HdfcSavingsCsvParserTest {

    private val sampleCsv = """
        HDFC BANK LIMITED
        Statement of account
        Date,Narration,Chq/Ref No.,Value Dt,Withdrawal Amt.,Deposit Amt.,Closing Balance
        01/04/24,UPI-SWIGGY-swiggy@ybl-401234567890-Payment,401234567890,01/04/24,150.00,,45320.00
        02/04/24,NEFT-SALARY-ACME CORP,NEFT0002,02/04/24,,50000.00,95320.00
        03/04/24,junk row with no valid date or amounts,,,,,
    """.trimIndent()

    @Test fun canParse_matchesHeader() {
        assertTrue(HdfcSavingsCsvParser.canParse(sampleCsv))
    }

    @Test fun canParse_rejectsUnrelatedText() {
        assertFalse(HdfcSavingsCsvParser.canParse("Txn Date,Debit,Credit,Balance"))
    }

    @Test fun parse_extractsWithdrawalAsExpense() {
        val items = HdfcSavingsCsvParser.parse(sampleCsv)
        val expense = items.first { it.direction == TransactionType.EXPENSE }
        assertEquals(LocalDate.of(2024, 4, 1), expense.date)
        assertEquals(15000L, expense.amountInPaise)
        assertEquals("UPI-SWIGGY-swiggy@ybl-401234567890-Payment", expense.description)
    }

    @Test fun parse_extractsDepositAsIncome() {
        val items = HdfcSavingsCsvParser.parse(sampleCsv)
        val income = items.first { it.direction == TransactionType.INCOME }
        assertEquals(LocalDate.of(2024, 4, 2), income.date)
        assertEquals(5000000L, income.amountInPaise)
        assertEquals("NEFT-SALARY-ACME CORP", income.description)
    }

    @Test fun parse_skipsMalformedRows() {
        val items = HdfcSavingsCsvParser.parse(sampleCsv)
        assertEquals(2, items.size)
    }

    @Test fun parse_returnsEmptyWhenNoHeaderFound() {
        assertTrue(HdfcSavingsCsvParser.parse("not a statement at all").isEmpty())
    }
}
