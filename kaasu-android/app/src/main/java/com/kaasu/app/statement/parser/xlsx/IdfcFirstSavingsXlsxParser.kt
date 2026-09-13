package com.kaasu.app.statement.parser.xlsx

import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.statement.model.StatementLineItem
import com.kaasu.app.statement.parser.StatementParser
import com.kaasu.app.statement.parser.csv.parseAmountToPaise
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * VERIFIED FORMAT — IDFC FIRST Bank savings-account "Account Statement" `.xlsx` export
 * (NetBanking/app → Statements → Download → Excel). Verified directly against a real sample
 * statement's raw XML (unzipped and inspected part-by-part, not guessed) — see
 * [XlsxTextExtractor], which turns that sheet into the pipe-delimited plain text this parser
 * consumes, matching every other [StatementParser]'s `fullText` contract.
 *
 * Header row (columns A..G, exact names, confirmed via the sheet's shared-string table):
 *   Transaction Date | Value Date | Particulars | Cheque No. | Debit | Credit | Balance
 *
 * Date format: dd-MMM-yyyy (e.g. "02-May-2026"). Debit/Credit: plain decimal; exactly one of the
 * two is non-blank per real transaction row. A trailing footer/summary block ("Total", "Total
 * number of Debits", "Total number of Credits", "End of the Statement", plus several fully blank
 * rows) follows the data — none of those rows have a value that parses as a Transaction Date, so
 * that failure is used as the skip signal rather than pattern-matching the footer text itself.
 *
 * Particulars is a semicolon-of-slashes micro-format that varies by transaction kind — a small
 * per-prefix mapper (not one regex) extracts both the user-facing description and, for UPI rows,
 * the UPI transaction ID (verified real examples, see [describeAndExtractUpiId]).
 */
object IdfcFirstSavingsXlsxParser : StatementParser {

    override val id: String = "idfc_first_savings_xlsx"
    override val displayName: String = "IDFC FIRST Bank Savings (XLSX)"

    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH)

    // Column indices per the header documented above.
    private const val COL_DATE = 0
    private const val COL_PARTICULARS = 2
    private const val COL_DEBIT = 4
    private const val COL_CREDIT = 5
    private const val MIN_COLUMNS = 6

    override fun canParse(sample: String): Boolean =
        sample.contains("Transaction Date", ignoreCase = true) &&
            sample.contains("Particulars", ignoreCase = true) &&
            sample.contains("Debit", ignoreCase = true) &&
            sample.contains("Credit", ignoreCase = true) &&
            sample.contains("Balance", ignoreCase = true)

    override fun parse(fullText: String): List<StatementLineItem> {
        val lines = fullText.lines()
        val headerIndex = lines.indexOfFirst {
            it.contains("Transaction Date", ignoreCase = true) && it.contains("Particulars", ignoreCase = true)
        }
        if (headerIndex == -1) return emptyList()

        val items = mutableListOf<StatementLineItem>()
        for (rawLine in lines.drop(headerIndex + 1)) {
            if (rawLine.isBlank()) continue
            val fields = rawLine.split("|")
            if (fields.size <= MIN_COLUMNS) continue

            // Footer/summary rows (blank Transaction Date, or literal "Total"/"End of the
            // Statement" text there) all fail this date parse — that's the skip signal.
            val date = runCatching { LocalDate.parse(fields[COL_DATE].trim(), DATE_FORMAT) }.getOrNull() ?: continue

            val debit = parseAmountToPaise(fields[COL_DEBIT])
            val credit = parseAmountToPaise(fields[COL_CREDIT])
            val (amount, direction) = when {
                debit != null && credit == null -> debit to TransactionType.EXPENSE
                credit != null && debit == null -> credit to TransactionType.INCOME
                // Defensive: every real row has exactly one of the two non-blank, but don't crash
                // or guess a direction if a row somehow has both/neither.
                else -> continue
            }

            val particulars = fields[COL_PARTICULARS].trim()
            val (description, upiTransactionId) = describeAndExtractUpiId(particulars)

            items += StatementLineItem(
                date = date,
                amountInPaise = amount,
                description = description,
                direction = direction,
                rawLineText = rawLine,
                upiTransactionId = upiTransactionId
            )
        }
        return items
    }

    // Verified real Particulars examples:
    //   UPI/DR/612288293666/VALLI M/CNRB/manicka/appale        -> "VALLI M", id "612288293666"
    //   UPI/CR/612900456914/ABDUL HA/UBIN/hakeema/aaliya       -> "ABDUL HA", id "612900456914"
    //   BLKIFT/OnscreenPayment/Salary                          -> "Salary"
    //   IFT-OPT/IFT/20261243117077/040526/1374                 -> "Fund Transfer"
    //   POS-VISA/YOUTUBEGOOGLE/613418180041/MUMBAI/18:46:11    -> "YOUTUBEGOOGLE"
    //   MONTHLY INTEREST CREDIT                                -> "Interest"
    private fun describeAndExtractUpiId(particulars: String): Pair<String, String?> {
        val segments = particulars.split("/")
        return when {
            segments.size >= 4 && segments[0].equals("UPI", ignoreCase = true) &&
                (segments[1].equals("DR", ignoreCase = true) || segments[1].equals("CR", ignoreCase = true)) ->
                segments[3].trim() to segments[2].trim()

            particulars.startsWith("BLKIFT", ignoreCase = true) -> "Salary" to null

            particulars.startsWith("IFT-OPT", ignoreCase = true) -> "Fund Transfer" to null

            particulars.startsWith("POS-VISA", ignoreCase = true) && segments.size >= 2 ->
                segments[1].trim() to null

            particulars.contains("INTEREST", ignoreCase = true) -> "Interest" to null

            else -> particulars to null
        }
    }
}
