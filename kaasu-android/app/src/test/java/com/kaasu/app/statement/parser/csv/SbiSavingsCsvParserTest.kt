package com.kaasu.app.statement.parser.csv

import com.kaasu.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SbiSavingsCsvParserTest {

    private val sampleCsv = """
        State Bank of India
        Account Statement
        Txn Date,Value Date,Description,Ref No./Cheque No.,Debit,Credit,Balance
        01/04/2024,01/04/2024,UPI/347123456789/ZOMATO/Payment,347123456789,320.50,,12500.00
        05/04/2024,05/04/2024,IMPS/SALARY CREDIT,IMPS9988,,60000.00,72500.00
    """.trimIndent()

    @Test fun canParse_matchesHeader() {
        assertTrue(SbiSavingsCsvParser.canParse(sampleCsv))
    }

    @Test fun canParse_rejectsHdfcHeader() {
        assertFalse(
            SbiSavingsCsvParser.canParse(
                "Date,Narration,Chq/Ref No.,Value Dt,Withdrawal Amt.,Deposit Amt.,Closing Balance"
            )
        )
    }

    @Test fun parse_extractsDebitAsExpense() {
        val items = SbiSavingsCsvParser.parse(sampleCsv)
        val expense = items.first { it.direction == TransactionType.EXPENSE }
        assertEquals(LocalDate.of(2024, 4, 1), expense.date)
        assertEquals(32050L, expense.amountInPaise)
        assertEquals("UPI/347123456789/ZOMATO/Payment", expense.description)
    }

    @Test fun parse_extractsCreditAsIncome() {
        val items = SbiSavingsCsvParser.parse(sampleCsv)
        val income = items.first { it.direction == TransactionType.INCOME }
        assertEquals(LocalDate.of(2024, 4, 5), income.date)
        assertEquals(6000000L, income.amountInPaise)
    }

    @Test fun parse_rowCount() {
        assertEquals(2, SbiSavingsCsvParser.parse(sampleCsv).size)
    }
}
