package com.kaasu.app.backfill

import com.kaasu.app.core.database.dao.TransactionDao
import com.kaasu.app.notification.classifier.CategoryRuleEngine
import com.kaasu.app.notification.model.RawNotification
import com.kaasu.app.notification.parser.TransactionParser
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Re-runs the current parser over already-stored transactions, using the `rawText` each one kept.
 *
 * Parser improvements otherwise only ever help *future* captures — every transaction already in the
 * database keeps whatever the parser of the day produced, which is why a history full of "Unknown"
 * merchants does not heal itself when the patterns improve.
 *
 * Two guarantees shape the whole class:
 *  - **Enrich only, never overwrite.** A field is written only where it is currently empty. A name
 *    or category the owner set by hand always wins over anything re-parsing produces.
 *  - **Never delete.** Rows the current parser would now reject (legacy promo junk, mandate
 *    pre-debit notices) are only *reported* by [countNonTransactions]; acting on them is a separate,
 *    explicit choice made in [ignoreNonTransactions].
 */
@Singleton
class TransactionBackfillManager @Inject constructor(
    private val dao: TransactionDao,
    private val parser: TransactionParser,
    private val categoryRuleEngine: CategoryRuleEngine,
) {

    data class Result(
        val scanned: Int,
        val merchantsFilled: Int,
        val categoriesFilled: Int,
        val notesFilled: Int,
        val nonTransactions: Int,
    )

    suspend fun run(): Result {
        val rows = dao.getAutoCapturedWithRawText()
        var merchants = 0
        var categories = 0
        var notes = 0
        var rejected = 0

        for (row in rows) {
            val rawText = row.rawText ?: continue
            val parsed = parser.parse(
                RawNotification(
                    packageName = row.sourceAppPackage ?: "",
                    appName = row.sourceAppName,
                    title = null,
                    // rawText is already the joined title+text+subText of the original capture,
                    // so it goes in whole rather than being split back into fields.
                    text = rawText,
                    subText = null,
                    postedAt = row.transactionTime,
                )
            )

            if (parsed == null) {
                // The current parser no longer considers this a transaction. Left untouched here.
                rejected++
                continue
            }

            val newMerchant = parsed.merchantName?.takeIf { row.merchantName.isNullOrBlank() }
            val newNote = parsed.note?.takeIf { row.note.isNullOrBlank() }

            // Category is resolved against the merchant we are about to store, not the old empty
            // one, so a row gains its name and its category in the same pass.
            val newCategory = if (row.categoryId == null) {
                val merchantForRules = row.merchantName?.takeIf { it.isNotBlank() } ?: parsed.merchantName
                categoryRuleEngine.classify(merchantForRules, row.sourceAppPackage)
                    ?: merchantForRules?.let { dao.getLearnedCategoryIdByMerchant(it) }
            } else {
                null
            }

            if (newMerchant == null && newNote == null && newCategory == null) continue

            dao.applyBackfill(
                id = row.id,
                merchantName = newMerchant,
                categoryId = newCategory,
                note = newNote,
                now = System.currentTimeMillis(),
            )

            if (newMerchant != null) merchants++
            if (newCategory != null) categories++
            if (newNote != null) notes++
        }

        return Result(
            scanned = rows.size,
            merchantsFilled = merchants,
            categoriesFilled = categories,
            notesFilled = notes,
            nonTransactions = rejected,
        )
    }

    /** How many stored rows the current parser would now reject. Reports only; changes nothing. */
    suspend fun countNonTransactions(): Int = collectNonTransactionIds().size

    /**
     * Marks rows the current parser rejects as ignored, so they stop counting toward spend totals.
     * Ignored rather than deleted — the row and its raw text survive, and the owner can reverse it.
     */
    suspend fun ignoreNonTransactions(): Int {
        val ids = collectNonTransactionIds()
        val now = System.currentTimeMillis()
        for (id in ids) dao.setIgnored(id, true, now)
        return ids.size
    }

    private suspend fun collectNonTransactionIds(): List<Long> =
        dao.getAutoCapturedWithRawText()
            .filter { row ->
                val rawText = row.rawText
                !row.isIgnored && rawText != null && parser.parse(
                    RawNotification(
                        packageName = row.sourceAppPackage ?: "",
                        appName = row.sourceAppName,
                        title = null,
                        text = rawText,
                        subText = null,
                        postedAt = row.transactionTime,
                    )
                ) == null
            }
            .map { it.id }
}
