package com.kaasu.app.statement.parser.csv

import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.statement.model.StatementLineItem
import com.kaasu.app.statement.parser.StatementParser
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ASSUMED FORMAT — generic Indian credit-card statement CSV export, representative of the
 * "download statement" style offered by SBI Card / HDFC / ICICI/Axis credit cards (this app's
 * confirmed real-world issuers, per notification/parser/AccountNotificationParser.kt and
 * CHANGELOG.md device-testing history). Best-effort, FIRST-PASS parser built WITHOUT a real
 * credit-card CSV sample — validate the header names, date format and column order below
 * against an actual export and adjust as needed.
 *
 * Expected header row (exact column names, in this order):
 *   Transaction Date,Description,Amount,Type
 *
 * Expected date format: dd/MM/yyyy   (e.g. "01/04/2024")
 * Amount: plain positive decimal, comma thousands separators allowed (e.g. "1,234.50").
 * Type: "Debit" or "Credit" (case-insensitive) — Debit = money spent (EXPENSE), Credit =
 * refund/payment received (INCOME).
 */
object GenericCreditCardCsvParser : StatementParser {

    override val id: String = "generic_credit_card_csv"
    override val displayName: String = "Credit Card Statement (CSV)"

    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    // Column indices per the header documented above.
    private const val COL_DATE = 0
    private const val COL_DESCRIPTION = 1
    private const val COL_AMOUNT = 2
    private const val COL_TYPE = 3
    private const val MIN_COLUMNS = 3

    override fun canParse(sample: String): Boolean =
        sample.contains("Transaction Date", ignoreCase = true) &&
            sample.contains("Description", ignoreCase = true) &&
            sample.contains("Amount", ignoreCase = true) &&
            sample.contains("Type", ignoreCase = true)

    override fun parse(fullText: String): List<StatementLineItem> {
        val lines = fullText.lines()
        val headerIndex = lines.indexOfFirst { it.contains("Transaction Date", ignoreCase = true) }
        if (headerIndex == -1) return emptyList()

        val items = mutableListOf<StatementLineItem>()
        for (rawLine in lines.drop(headerIndex + 1)) {
            if (rawLine.isBlank()) continue
            val fields = splitCsvLine(rawLine)
            if (fields.size <= MIN_COLUMNS) continue

            val date = runCatching { LocalDate.parse(fields[COL_DATE], DATE_FORMAT) }.getOrNull() ?: continue
            val description = fields[COL_DESCRIPTION]
            val amount = parseAmountToPaise(fields[COL_AMOUNT]) ?: continue
            val direction = if (fields[COL_TYPE].contains("credit", ignoreCase = true)) {
                TransactionType.INCOME
            } else {
                TransactionType.EXPENSE
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
