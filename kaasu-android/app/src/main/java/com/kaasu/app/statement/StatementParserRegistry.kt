package com.kaasu.app.statement

import com.kaasu.app.statement.parser.StatementParser
import com.kaasu.app.statement.parser.csv.GenericCreditCardCsvParser
import com.kaasu.app.statement.parser.csv.HdfcSavingsCsvParser
import com.kaasu.app.statement.parser.csv.SbiSavingsCsvParser
import com.kaasu.app.statement.parser.pdf.GpayStatementPdfParser
import com.kaasu.app.statement.parser.pdf.HdfcSavingsPdfParser
import com.kaasu.app.statement.parser.xlsx.IdfcFirstSavingsXlsxParser
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ordered list of every registered [StatementParser]. First match wins — see each parser's
 * canParse() for its exact fingerprint. Add a new (bank, format) parser here as it's built.
 *
 * [IdfcFirstSavingsXlsxParser] and [GpayStatementPdfParser] are verified against real sample
 * statements from the app owner (see CHANGELOG.md); the other four are first-pass, unvalidated
 * guesses built without a real sample of that bank's export — see each one's own doc comment.
 */
@Singleton
class StatementParserRegistry @Inject constructor() {

    val parsers: List<StatementParser> = listOf(
        HdfcSavingsCsvParser,
        SbiSavingsCsvParser,
        GenericCreditCardCsvParser,
        HdfcSavingsPdfParser,
        IdfcFirstSavingsXlsxParser,
        GpayStatementPdfParser,
    )

    fun findParser(sample: String): StatementParser? = parsers.firstOrNull { it.canParse(sample) }
}
