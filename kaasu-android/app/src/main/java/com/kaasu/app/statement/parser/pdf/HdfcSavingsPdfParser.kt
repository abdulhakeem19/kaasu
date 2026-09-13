package com.kaasu.app.statement.parser.pdf

import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.statement.model.StatementLineItem
import com.kaasu.app.statement.parser.StatementParser
import com.kaasu.app.statement.parser.csv.parseAmountToPaise
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ASSUMED FORMAT — HDFC Bank savings/current account e-statement PDF, TEXT-BASED (selectable
 * text) only — scanned/image statements are out of scope (no OCR). Best-effort, FIRST-PASS
 * parser built WITHOUT a real HDFC PDF sample — validate and adjust against an actual export.
 *
 * PdfBox-Android's text extraction loses the original table's column alignment, so rather than
 * assuming column positions, this matches one transaction per line with the row's cells
 * collapsed to single-space-separated tokens, in this assumed order:
 *
 *   <dd/MM/yy> <narration text...> <Dr|Cr> <amount> <closing balance>
 *
 * Example line: "01/04/24 UPI-SWIGGY-swiggy@ybl-401234567890-Payment Dr 150.00 45,320.00"
 *
 * canParse() fingerprints on the bank name + a statement-header phrase (present near the top of
 * the extracted text) rather than the transaction-line shape above, since the latter only
 * appears further down, after the account summary section.
 */
object HdfcSavingsPdfParser : StatementParser {

    override val id: String = "hdfc_savings_pdf"
    override val displayName: String = "HDFC Bank Statement (PDF)"

    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy")

    // Group 1: date, Group 2: narration (non-greedy), Group 3: Dr/Cr, Group 4: amount, Group 5: balance.
    private val LINE_PATTERN = Regex(
        """(\d{2}/\d{2}/\d{2})\s+(.+?)\s+(Dr|Cr)\s+([\d,]+\.\d{2})\s+([\d,]+\.\d{2})\s*$"""
    )

    override fun canParse(sample: String): Boolean =
        sample.contains("HDFC BANK", ignoreCase = true) &&
            sample.contains("Statement", ignoreCase = true) &&
            sample.contains("Account", ignoreCase = true)

    override fun parse(fullText: String): List<StatementLineItem> {
        val items = mutableListOf<StatementLineItem>()
        for (rawLine in fullText.lines()) {
            val trimmed = rawLine.trim()
            if (trimmed.isBlank()) continue
            val match = LINE_PATTERN.find(trimmed) ?: continue
            val (dateStr, narration, drCr, amountStr) = match.destructured

            val date = runCatching { LocalDate.parse(dateStr, DATE_FORMAT) }.getOrNull() ?: continue
            val amount = parseAmountToPaise(amountStr) ?: continue
            val direction = if (drCr.equals("Cr", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE

            items += StatementLineItem(
                date = date,
                amountInPaise = amount,
                description = narration.trim(),
                direction = direction,
                rawLineText = trimmed
            )
        }
        return items
    }
}
