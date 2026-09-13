package com.kaasu.app.statement.parser.pdf

import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.statement.model.StatementLineItem
import com.kaasu.app.statement.parser.StatementParser
import com.kaasu.app.statement.parser.csv.parseAmountToPaise
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * VERIFIED FORMAT — Google Pay "Transaction statement" PDF export (Google Pay app → Profile →
 * Settings → Reports & statements → Transaction statement). Verified against a real 54-page
 * sample statement by running it through PdfBox's `PDFTextStripper` (the same core
 * text-extraction class `pdfbox-android`'s [PdfTextExtractor] wraps) and cross-checked against
 * `pdftotext` — text-based/selectable-text PDFs only, same scope limitation as every other PDF
 * parser here.
 *
 * One repeating block per transaction, six non-blank lines, in this EXACT real order (note: this
 * is not the order a casual look at the PDF's visual columns would suggest — real extraction
 * puts the time line right after the date, and the amount AFTER the settlement line, not before
 * it):
 *   "DD Month, YYYY"                                  (date)
 *   "HH:MM AM/PM"                                      (time — not currently used beyond the date)
 *   "Paid to <name>" | "Received from <name>" | "Self transfer to/from <bank>"   (verb line)
 *   "UPI Transaction ID: <digits>"                      (anchor)
 *   "Paid by <bank/card>" | "Paid to <bank/card>"       (settlement line — NOT direction-bearing;
 *                                                         a credit's settlement confusingly also
 *                                                         starts with "Paid to", so this line is
 *                                                         ignored entirely for direction)
 *   "₹<amount>"                                         (Indian-grouped thousands allowed)
 *
 * "Self transfer to/from <bank>" is a real, verified third verb variant: the user moving money
 * between their own linked accounts (e.g. IDFC FIRST -> Union Bank). GPay's own statement note
 * says these are excluded from the Sent/Received totals, so this parser skips them too rather
 * than mis-booking an inter-own-account transfer as income or spend.
 *
 * Parsing is anchored on the `UPI Transaction ID:` line (present on every real transaction block,
 * including self-transfers) and window-scans a few lines to either side for the date/verb/amount,
 * rather than assuming fixed relative offsets — the real extraction happened to have zero blank
 * lines between elements in the verified sample, but nothing here assumes that stays true.
 */
object GpayStatementPdfParser : StatementParser {

    override val id: String = "gpay_statement_pdf"
    override val displayName: String = "Google Pay Statement (PDF)"

    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.ENGLISH)
    private val DATE_PATTERN = Regex("""^\d{1,2}\s+[A-Za-z]+,?\s+\d{4}$""")
    private val VERB_PATTERN = Regex("""^(Paid to|Received from|Self transfer to|Self transfer from)\s+(.+)$""")
    private val ANCHOR_PATTERN = Regex("""^UPI Transaction ID:\s*(\d+)$""")
    private val AMOUNT_PATTERN = Regex("""^₹\s*[\d,]+(?:\.\d+)?$""")

    // How far to scan to either side of a UPI-ID anchor line for its date/verb/amount.
    private const val WINDOW = 4

    override fun canParse(sample: String): Boolean =
        sample.contains("Google Pay", ignoreCase = true) && sample.contains("UPI Transaction ID", ignoreCase = true)

    override fun parse(fullText: String): List<StatementLineItem> {
        val lines = fullText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val items = mutableListOf<StatementLineItem>()

        for (i in lines.indices) {
            val anchorMatch = ANCHOR_PATTERN.find(lines[i]) ?: continue
            val upiId = anchorMatch.groupValues[1]

            val dateIndex = (i - 1 downTo maxOf(0, i - WINDOW)).firstOrNull { DATE_PATTERN.matches(lines[it]) }
                ?: continue
            val date = runCatching { LocalDate.parse(lines[dateIndex], DATE_FORMAT) }.getOrNull() ?: continue

            val verbMatch = (dateIndex + 1 until minOf(i, dateIndex + 1 + WINDOW))
                .asSequence()
                .mapNotNull { idx -> VERB_PATTERN.find(lines[idx]) }
                .firstOrNull() ?: continue
            val verb = verbMatch.groupValues[1]
            val counterparty = verbMatch.groupValues[2].trim()

            val direction = when {
                verb.equals("Paid to", ignoreCase = true) -> TransactionType.EXPENSE
                verb.equals("Received from", ignoreCase = true) -> TransactionType.INCOME
                // "Self transfer to/from" — movement between the user's own linked accounts, not
                // real income or spend (GPay's own statement note excludes these from totals too).
                else -> continue
            }

            val amountIndex = (i + 1..minOf(lines.lastIndex, i + WINDOW)).firstOrNull { AMOUNT_PATTERN.matches(lines[it]) }
                ?: continue
            val amount = parseAmountToPaise(lines[amountIndex]) ?: continue

            val rawBlock = (dateIndex..amountIndex).joinToString(" | ") { lines[it] }

            items += StatementLineItem(
                date = date,
                amountInPaise = amount,
                description = counterparty,
                direction = direction,
                rawLineText = rawBlock,
                upiTransactionId = upiId
            )
        }
        return items
    }
}
