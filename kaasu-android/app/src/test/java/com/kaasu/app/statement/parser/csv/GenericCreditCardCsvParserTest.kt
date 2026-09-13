package com.kaasu.app.statement.parser.csv

import com.kaasu.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GenericCreditCardCsvParserTest {

    private val sampleCsv = """
        SBI Card Statement
        Transaction Date,Description,Amount,Type
        01/04/2024,AMAZON.IN,1499.00,Debit
        03/04/2024,PAYMENT RECEIVED - THANK YOU,5000.00,Credit
    """.trimIndent()

    @Test fun canParse_matchesHeader() {
        assertTrue(GenericCreditCardCsvParser.canParse(sampleCsv))
    }

    @Test fun canParse_rejectsSavingsHeader() {
        assertFalse(
            GenericCreditCardCsvParser.canParse(
                "Txn Date,Value Date,Description,Ref No./Cheque No.,Debit,Credit,Balance"
            )
        )
    }

    @Test fun parse_debitIsExpense() {
        val items = GenericCreditCardCsvParser.parse(sampleCsv)
        val expense = items.first { it.direction == TransactionType.EXPENSE }
        assertEquals(LocalDate.of(2024, 4, 1), expense.date)
        assertEquals(149900L, expense.amountInPaise)
        assertEquals("AMAZON.IN", expense.description)
    }

    @Test fun parse_creditIsIncome() {
        val items = GenericCreditCardCsvParser.parse(sampleCsv)
        val income = items.first { it.direction == TransactionType.INCOME }
        assertEquals(500000L, income.amountInPaise)
    }

    @Test fun parse_rowCount() {
        assertEquals(2, GenericCreditCardCsvParser.parse(sampleCsv).size)
    }
}
