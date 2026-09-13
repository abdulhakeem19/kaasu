package com.kaasu.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kaasu.app.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions WHERE isIgnored = 0 ORDER BY transactionTime DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    // Live version of getById — re-emits whenever this transaction is edited, so the detail screen
    // reflects updates immediately instead of holding a one-shot snapshot.
    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE rawTextHash = :hash LIMIT 1")
    suspend fun getByHash(hash: String): TransactionEntity?

    @Query("SELECT rawText FROM transactions WHERE id = :id")
    suspend fun getRawTextById(id: Long): String?

    @Query("UPDATE transactions SET isRecurring = :isRecurring, updatedAt = :now WHERE id = :id")
    suspend fun setRecurring(id: Long, isRecurring: Boolean, now: Long)

    @Query("UPDATE transactions SET categoryId = :categoryId, updatedAt = :now WHERE id = :id")
    suspend fun setCategory(id: Long, categoryId: Long?, now: Long)

    // Bulk-rename every existing transaction whose merchant matches (case-insensitive)
    @Query("UPDATE transactions SET merchantName = :newName, updatedAt = :now WHERE merchantName = :oldName COLLATE NOCASE")
    suspend fun renameMerchant(oldName: String, newName: String, now: Long)

    @Query("UPDATE transactions SET isIgnored = :isIgnored, updatedAt = :now WHERE id = :id")
    suspend fun setIgnored(id: Long, isIgnored: Boolean, now: Long)

    // Marking duplicate also hides it (isIgnored) so it leaves lists and totals; un-marking restores both.
    @Query("UPDATE transactions SET isDuplicate = :isDuplicate, isIgnored = :isDuplicate, updatedAt = :now WHERE id = :id")
    suspend fun setDuplicate(id: Long, isDuplicate: Boolean, now: Long)

    @Query("SELECT * FROM transactions WHERE isDuplicate = 1 ORDER BY transactionTime DESC")
    fun observeDuplicates(): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insertAll(transactions: List<TransactionEntity>)

    // Atomically hide the parent and insert its child slices.
    @androidx.room.Transaction
    suspend fun splitTransaction(parentId: Long, children: List<TransactionEntity>, now: Long) {
        setIgnored(parentId, true, now)
        insertAll(children)
    }

    // Transactions that still need a category (the "Needs a tag" queue), newest first
    @Query("SELECT * FROM transactions WHERE categoryId IS NULL AND isIgnored = 0 ORDER BY transactionTime DESC")
    fun getUncategorized(): Flow<List<TransactionEntity>>

    // Category this merchant has most often been filed under — the "learn from my history" step.
    // Most-frequent rather than most-recent so one misfiled transaction cannot re-teach the wrong
    // category, with recency only breaking ties. NOCASE because captured merchant casing varies
    // between channels ("SWIGGY" from SMS, "Swiggy" from the notification).
    @Query("""
        SELECT categoryId FROM transactions
        WHERE merchantName = :merchant COLLATE NOCASE
          AND categoryId IS NOT NULL
          AND isIgnored = 0
        GROUP BY categoryId
        ORDER BY COUNT(*) DESC, MAX(transactionTime) DESC
        LIMIT 1
    """)
    suspend fun getLearnedCategoryIdByMerchant(merchant: String): Long?

    // Most recent account used for a given merchant — drives "same merchant → same account" linking
    @Query("""
        SELECT accountId FROM transactions
        WHERE merchantName = :merchant AND accountId IS NOT NULL
        ORDER BY transactionTime DESC LIMIT 1
    """)
    suspend fun getLatestAccountIdByMerchant(merchant: String): Long?

    @Query("""
        SELECT * FROM transactions
        WHERE amountInPaise = :amountInPaise AND type = :type AND transactionTime >= :since
        ORDER BY transactionTime DESC
    """)
    suspend fun getRecentByAmountAndType(
        amountInPaise: Long,
        type: String,
        since: Long
    ): List<TransactionEntity>

    // Same-day + same amount/type lookup used by StatementImportManager's Tier 2 dedup (catches
    // cross-source duplicates against transactions already captured via notification/SMS).
    // Analogous to getRecentByAmountAndType but scoped to a date range instead of "since".
    @Query("""
        SELECT * FROM transactions
        WHERE transactionTime BETWEEN :startOfDayMillis AND :endOfDayMillis
          AND amountInPaise = :amountInPaise AND type = :type
        ORDER BY transactionTime DESC
    """)
    suspend fun getByDateRangeAmountAndType(
        startOfDayMillis: Long,
        endOfDayMillis: Long,
        amountInPaise: Long,
        type: String
    ): List<TransactionEntity>

    // Tier 1.5 statement-import dedup (StatementImportManager): the same real-world UPI payment
    // can show up in more than one capture source — the IDFC XLSX statement (its own reference
    // number embedded in the Particulars text), the GPay PDF statement ("UPI Transaction ID:
    // ..."), and often the original bank SMS/notification rawText too — all embedding the same
    // numeric reference. A rawText substring match on that reference is a far more reliable
    // cross-source signal than same-day/merchant-similarity fuzzy matching (Tier 2). pattern is
    // always a purely numeric UPI transaction ID extracted by a statement parser, never raw user
    // input, so there's no LIKE-wildcard-injection concern from this call site.
    // Auto-captured rows that still hold their raw text — the input set for a re-parse backfill.
    // Manual entries are excluded: the owner typed those, so re-parsing must never overwrite them.
    @Query("""
        SELECT * FROM transactions
        WHERE isManual = 0 AND rawText IS NOT NULL AND rawText <> ''
        ORDER BY transactionTime DESC
    """)
    suspend fun getAutoCapturedWithRawText(): List<TransactionEntity>

    // Backfill writes every field it can improve in one statement. Each value is passed through
    // COALESCE at the call site rather than here, so a null argument means "leave it alone".
    @Query("""
        UPDATE transactions
        SET merchantName = COALESCE(:merchantName, merchantName),
            categoryId   = COALESCE(:categoryId, categoryId),
            note         = COALESCE(:note, note),
            updatedAt    = :now
        WHERE id = :id
    """)
    suspend fun applyBackfill(id: Long, merchantName: String?, categoryId: Long?, note: String?, now: Long)

    @Query("SELECT * FROM transactions WHERE rawText LIKE '%' || :pattern || '%'")
    suspend fun findByRawTextContaining(pattern: String): List<TransactionEntity>

    @Query("""
        SELECT * FROM transactions
        WHERE transactionTime BETWEEN :startTime AND :endTime AND isIgnored = 0
        ORDER BY transactionTime DESC
    """)
    fun getByDateRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId AND isIgnored = 0 ORDER BY transactionTime DESC")
    fun getByCategory(categoryId: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE (merchantName LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%')
        AND isIgnored = 0
        ORDER BY transactionTime DESC
    """)
    fun search(query: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT COALESCE(SUM(amountInPaise), 0) FROM transactions
        WHERE type = 'EXPENSE' AND transactionTime BETWEEN :startTime AND :endTime AND isIgnored = 0
    """)
    suspend fun getTotalSpendInPaise(startTime: Long, endTime: Long): Long

    @Query("""
        SELECT COALESCE(SUM(amountInPaise), 0) FROM transactions
        WHERE type = 'INCOME' AND transactionTime BETWEEN :startTime AND :endTime AND isIgnored = 0
    """)
    suspend fun getTotalIncomeInPaise(startTime: Long, endTime: Long): Long

    // Called by DeleteAllDataUseCase — hard delete per privacy policy
    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    // Full snapshot (includes ignored/split-parent rows) for backup
    @Query("SELECT * FROM transactions")
    suspend fun getAllForBackup(): List<TransactionEntity>
}
