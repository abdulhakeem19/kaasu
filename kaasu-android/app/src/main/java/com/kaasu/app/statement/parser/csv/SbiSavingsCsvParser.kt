package com.kaasu.app.statement.parser.csv

import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.statement.model.StatementLineItem
import com.kaasu.app.statement.parser.StatementParser
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ASSUMED FORMAT — SBI (State Bank of India) savings/current account "Statement" CSV export
 * (YONO / net-banking → Account Statement → Download → CSV). Best-effort, FIRST-PASS parser
 * built WITHOUT a real SBI CSV sample — validate the header names, date format and column
 * order below against an actual export and adjust as needed.
 *
 * Expected header row (exact column names, in this order):
 *   Txn Date,Value Date,Description,Ref No./Cheque No.,Debit,Credit,Balance
 *
 * Expected date format: dd/MM/yyyy   (e.g. "01/04/2024")
 * Debit / Credit: plain decimal, comma thousands separators allowed (e.g. "1,234.50"); exactly
 * one of the two is expected to be non-blank per row.
 */
object SbiSavingsCsvParser : StatementParser {

    override val id: String = "sbi_savings_csv"
    override val displayName: String = "SBI Savings/Current (CSV)"

    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    // Column indices per the header documented above.
    private const val COL_DATE = 0
    private const val COL_DESCRIPTION = 2
    private const val COL_DEBIT = 4
    private const val COL_CREDIT = 5
    private const val MIN_COLUMNS = 6

    override fun canParse(sample: String): Boolean =
        sample.contains("Txn Date", ignoreCase = true) &&
            sample.contains("Debit", ignoreCase = true) &&
            sample.contains("Credit", ignoreCase = true) &&
            sample.contains("Balance", ignoreCase = true)

    override fun parse(fullText: String): List<StatementLineItem> {
        val lines = fullText.lines()
        val headerIndex = lines.indexOfFirst { it.contains("Txn Date", ignoreCase = true) }
        if (headerIndex == -1) return emptyList()

        val items = mutableListOf<StatementLineItem>()
        for (rawLine in lines.drop(headerIndex + 1)) {
            if (rawLine.isBlank()) continue
            val fields = splitCsvLine(rawLine)
            if (fields.size <= MIN_COLUMNS) continue

            val date = runCatching { LocalDate.parse(fields[COL_DATE], DATE_FORMAT) }.getOrNull() ?: continue
            val description = fields[COL_DESCRIPTION]
            val debit = parseAmountToPaise(fields[COL_DEBIT])
            val credit = parseAmountToPaise(fields[COL_CREDIT])

            val (amount, direction) = when {
                debit != null -> debit to TransactionType.EXPENSE
                credit != null -> credit to TransactionType.INCOME
                else -> continue
            }

            items += StatementLineItem(
                date = date,
                amountInPaise = amount,
                description = description,
                direction = direction,
                rawLineText = rawLine
            )
        }
        return items
    }
}
