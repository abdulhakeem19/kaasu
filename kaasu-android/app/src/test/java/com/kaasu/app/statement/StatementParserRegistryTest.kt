package com.kaasu.app.statement

import com.kaasu.app.statement.parser.csv.GenericCreditCardCsvParser
import com.kaasu.app.statement.parser.csv.HdfcSavingsCsvParser
import com.kaasu.app.statement.parser.csv.SbiSavingsCsvParser
import com.kaasu.app.statement.parser.pdf.GpayStatementPdfParser
import com.kaasu.app.statement.parser.pdf.HdfcSavingsPdfParser
import com.kaasu.app.statement.parser.xlsx.IdfcFirstSavingsXlsxParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatementParserRegistryTest {

    private val registry = StatementParserRegistry()

    @Test fun findParser_dispatchesHdfcSavingsCsv() {
        val sample = "Date,Narration,Chq/Ref No.,Value Dt,Withdrawal Amt.,Deposit Amt.,Closing Balance"
        assertEquals(HdfcSavingsCsvParser, registry.findParser(sample))
    }

    @Test fun findParser_dispatchesSbiSavingsCsv() {
        val sample = "Txn Date,Value Date,Description,Ref No./Cheque No.,Debit,Credit,Balance"
        assertEquals(SbiSavingsCsvParser, registry.findParser(sample))
    }

    @Test fun findParser_dispatchesGenericCreditCardCsv() {
        val sample = "Transaction Date,Description,Amount,Type"
        assertEquals(GenericCreditCardCsvParser, registry.findParser(sample))
    }

    @Test fun findParser_dispatchesHdfcSavingsPdf() {
        val sample = "HDFC BANK LIMITED Statement of Account for account number 401234567890"
        assertEquals(HdfcSavingsPdfParser, registry.findParser(sample))
    }

    @Test fun findParser_dispatchesIdfcFirstSavingsXlsx() {
        val sample = "Transaction Date|Value Date|Particulars|Cheque No.|Debit|Credit|Balance"
        assertEquals(IdfcFirstSavingsXlsxParser, registry.findParser(sample))
    }

    @Test fun findParser_dispatchesGpayStatementPdf() {
        val sample = "Google Pay\nTransaction statement\nUPI Transaction ID: 700000000001"
        assertEquals(GpayStatementPdfParser, registry.findParser(sample))
    }

    @Test fun findParser_returnsNullForUnrecognizedFormat() {
        assertNull(registry.findParser("Just some random text with no known statement header"))
    }

    @Test fun parsers_haveDistinctDisplayNames() {
        val names = registry.parsers.map { it.displayName }
        assertEquals(names.size, names.toSet().size)
    }
}
