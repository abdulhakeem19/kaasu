package com.kaasu.app.statement.parser.csv

import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.statement.model.StatementLineItem
import com.kaasu.app.statement.parser.StatementParser
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ASSUMED FORMAT — HDFC Bank savings/current account "Statement" CSV export (NetBanking →
 * Statements → Download as CSV). This is a best-effort, FIRST-PASS parser built WITHOUT a real
 * HDFC CSV sample — validate the header names, date format and column order below against an
 * actual export and adjust as needed.
 *
 * Expected header row (exact column names, in this order):
 *   Date,Narration,Chq/Ref No.,Value Dt,Withdrawal Amt.,Deposit Amt.,Closing Balance
 *
 * Expected date format: dd/MM/yy   (e.g. "01/04/24")
 * Withdrawal Amt. / Deposit Amt.: plain decimal, comma thousands separators allowed
 * (e.g. "1,234.50"); exactly one of the two is expected to be non-blank per row — the other is
 * blank because it doesn't apply to that row's direction.
 */
object HdfcSavingsCsvParser : StatementParser {

    override val id: String = "hdfc_savings_csv"
    override val displayName: String = "HDFC Bank Savings/Current (CSV)"

    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy")

    // Column indices per the header documented above.
    private const val COL_DATE = 0
    private const val COL_NARRATION = 1
    private const val COL_WITHDRAWAL = 4
    private const val COL_DEPOSIT = 5
    private const val MIN_COLUMNS = 6

    override fun canParse(sample: String): Boolean =
        sample.contains("Narration", ignoreCase = true) &&
            sample.contains("Withdrawal Amt", ignoreCase = true) &&
            sample.contains("Deposit Amt", ignoreCase = true) &&
            sample.contains("Closing Balance", ignoreCase = true)

    override fun parse(fullText: String): List<StatementLineItem> {
        val lines = fullText.lines()
        val headerIndex = lines.indexOfFirst { it.contains("Narration", ignoreCase = true) }
        if (headerIndex == -1) return emptyList()

        val items = mutableListOf<StatementLineItem>()
        for (rawLine in lines.drop(headerIndex + 1)) {
            if (rawLine.isBlank()) continue
            val fields = splitCsvLine(rawLine)
            if (fields.size <= MIN_COLUMNS) continue

            val date = runCatching { LocalDate.parse(fields[COL_DATE], DATE_FORMAT) }.getOrNull() ?: continue
            val narration = fields[COL_NARRATION]
            val withdrawal = parseAmountToPaise(fields[COL_WITHDRAWAL])
            val deposit = parseAmountToPaise(fields[COL_DEPOSIT])

            val (amount, direction) = when {
                withdrawal != null -> withdrawal to TransactionType.EXPENSE
                deposit != null -> deposit to TransactionType.INCOME
                else -> continue
            }

            items += StatementLineItem(
                date = date,
                amountInPaise = amount,
                description = narration,
                direction = direction,
                rawLineText = rawLine
            )
        }
        return items
    }
}
