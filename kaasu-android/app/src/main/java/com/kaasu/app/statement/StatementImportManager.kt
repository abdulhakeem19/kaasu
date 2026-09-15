package com.kaasu.app.statement

import android.content.Context
import android.net.Uri
import com.kaasu.app.core.util.Hashing
import com.kaasu.app.core.util.MerchantSimilarity
import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.repository.TransactionRepository
import com.kaasu.app.notification.classifier.CategoryRuleEngine
import com.kaasu.app.statement.model.ImportResult
import com.kaasu.app.statement.model.StatementLineItem
import com.kaasu.app.statement.parser.pdf.PdfTextExtractor
import com.kaasu.app.statement.parser.xlsx.XlsxTextExtractor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the whole user-initiated statement-import flow: pick a file → [preview] (parse +
 * two-tier dedup check, no writes) → user reviews the new-vs-already-tracked split → [commit]
 * (inserts the reviewed "new" subset). Mirrors [com.kaasu.app.core.backup.BackupManager]'s
 * plain-suspend-call-on-viewModelScope shape for a file-picker-driven bulk operation — statement
 * sizes (a few hundred to low thousands of lines) don't warrant a WorkManager worker.
 *
 * Deliberately does NOT reuse [com.kaasu.app.notification.parser.TransactionParser] or
 * [com.kaasu.app.notification.duplicate.DuplicateChecker]: those are tuned for narrative-sentence
 * notification/SMS text and near-simultaneous cross-channel echoes, whereas a statement line is
 * already structured (date + amount + description) and only needs a same-day dedup window.
 * Still lands in the same [com.kaasu.app.core.database.entity.TransactionEntity] table via the
 * same [CategoryRuleEngine] for auto-categorization.
 */
@Singleton
class StatementImportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val registry: StatementParserRegistry,
    private val pdfTextExtractor: PdfTextExtractor,
    private val xlsxTextExtractor: XlsxTextExtractor,
    private val transactionRepository: TransactionRepository,
    private val categoryRuleEngine: CategoryRuleEngine,
) {
    // Marks every imported row's source, distinguishing it from notification/SMS capture and
    // manual entry. sourceAppPackage/sourceAppName are free nullable strings on TransactionEntity
    // already, so no schema migration is needed for this.
    private companion object {
        const val SOURCE_APP_PACKAGE = "statement_import"
        const val SAMPLE_CHAR_LIMIT = 4_000
        const val XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

        // .xlsx (and any zip) file starts with this two-byte "PK" local-file-header magic.
        const val ZIP_MAGIC_BYTE_0 = 0x50.toByte()
        const val ZIP_MAGIC_BYTE_1 = 0x4B.toByte()
    }

    /** Parses [uri] and runs the two-tier dedup check. Performs no database writes. */
    suspend fun preview(uri: Uri, mimeType: String?): ImportResult {
        val fullText = extractText(uri, mimeType)
        val sample = fullText.take(SAMPLE_CHAR_LIMIT)
        val parser = registry.findParser(sample)
            ?: throw UnrecognizedStatementFormatException(registry.parsers.map { it.displayName })

        val allItems = parser.parse(fullText)
        val newItems = mutableListOf<StatementLineItem>()
        var duplicateCount = 0
        for (item in allItems) {
            if (isDuplicate(item)) duplicateCount++ else newItems += item
        }

        return ImportResult(
            bankDisplayName = parser.displayName,
            totalFound = allItems.size,
            newItems = newItems,
            duplicateCount = duplicateCount,
        )
    }

    /** Inserts the "new" subset from a previously computed [ImportResult]. Returns how many were inserted. */
    suspend fun commit(result: ImportResult): Int {
        val now = System.currentTimeMillis()
        for (item in result.newItems) {
            val transaction = Transaction(
                amountInPaise = item.amountInPaise,
                type = item.direction,
                merchantName = item.description,
                categoryId = categoryRuleEngine.classifyCategory(item.description, sourceAppPackage = null),
                sourceAppPackage = SOURCE_APP_PACKAGE,
                sourceAppName = "${result.bankDisplayName} Statement",
                paymentMode = null,
                rawTextHash = Hashing.sha256Prefix(item.rawLineText),
                transactionTime = item.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                createdAt = now,
                updatedAt = now,
                isManual = false,
                note = null,
            )
            transactionRepository.insertParsed(transaction, rawText = item.rawLineText)
        }
        return result.newItems.size
    }

    private fun extractText(uri: Uri, mimeType: String?): String {
        if (mimeType == "application/pdf") return pdfTextExtractor.extractText(uri)
        if (mimeType == XLSX_MIME_TYPE) return xlsxTextExtractor.extractText(uri)

        // Some file-picker/content providers misreport an .xlsx's mime type as a generic
        // "application/octet-stream" (or omit it entirely) instead of the XLSX type above. Sniff
        // the zip magic bytes ("PK") to disambiguate .xlsx-as-zip from plain CSV text rather than
        // trusting mimeType alone — cheap, and this app only needs to distinguish these two cases.
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Could not read the selected file")
        return if (bytes.size >= 2 && bytes[0] == ZIP_MAGIC_BYTE_0 && bytes[1] == ZIP_MAGIC_BYTE_1) {
            xlsxTextExtractor.extractText(bytes)
        } else {
            bytes.toString(Charsets.UTF_8)
        }
    }

    // Tier 1: exact-hash lookup — catches literal re-imports / overlapping statement-cycle
    // duplicate lines. Tier 1.5 (only reached if Tier 1 doesn't match, and only when the item
    // carries a UPI transaction ID): substring match against every existing transaction's stored
    // rawText — the same real-world UPI payment can appear in more than one capture source (e.g.
    // both the IDFC XLSX statement and the GPay PDF statement embed the same reference number,
    // and bank SMS/notification rawText commonly does too), and that's a far more reliable
    // cross-source signal than the fuzzy merchant-similarity match Tier 2 falls back to. Tier 2
    // (only reached if neither above matches, e.g. the item has no UPI ID at all — IDFC's
    // BLKIFT/IFT-OPT/interest rows): same-calendar-day + same amount/type + merchant-similarity —
    // catches cross-source duplicates against rows already captured via notification/SMS.
    private suspend fun isDuplicate(item: StatementLineItem): Boolean {
        val hash = Hashing.sha256Prefix(item.rawLineText)
        if (transactionRepository.getByHash(hash) != null) return true

        val upiId = item.upiTransactionId
        if (upiId != null && transactionRepository.findByRawTextContaining(upiId).isNotEmpty()) return true

        val zone = ZoneId.systemDefault()
        val startOfDay = item.date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = item.date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val candidates = transactionRepository.getByDateRangeAmountAndType(
            startOfDayMillis = startOfDay,
            endOfDayMillis = endOfDay,
            amountInPaise = item.amountInPaise,
            type = item.direction.name,
        )
        return candidates.any { existing -> MerchantSimilarity.areSimilar(existing.merchantName, item.description) }
    }
}

/** Thrown by [StatementImportManager.preview] when no registered parser recognizes the file. */
class UnrecognizedStatementFormatException(supportedFormats: List<String>) : Exception(
    "Couldn't recognize this file's format. Kaasu currently supports: ${supportedFormats.joinToString(", ")}."
)
