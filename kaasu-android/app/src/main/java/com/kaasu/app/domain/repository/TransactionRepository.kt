package com.kaasu.app.domain.repository

import com.kaasu.app.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

// One slice of a split transaction: a portion of the amount assigned to a category.
data class SplitSlice(val amountInPaise: Long, val categoryId: Long?)

interface TransactionRepository {
    fun getAll(): Flow<List<Transaction>>
    fun getByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>>
    fun getByCategory(categoryId: Long): Flow<List<Transaction>>
    fun search(query: String): Flow<List<Transaction>>
    suspend fun getById(id: Long): Transaction?
    // Live single-transaction stream for the detail screen (re-emits on edit).
    fun observeById(id: Long): Flow<Transaction?>
    suspend fun getByHash(hash: String): Transaction?
    // Tier 1.5 statement-import dedup: substring match against stored rawText (see DAO for why).
    suspend fun findByRawTextContaining(pattern: String): List<Transaction>
    suspend fun getRawText(id: Long): String?
    suspend fun getLatestAccountIdByMerchant(merchant: String): Long?

    // Category this merchant has most often been filed under, or null if never categorised.
    suspend fun getLearnedCategoryIdByMerchant(merchant: String): Long?
    suspend fun setRecurring(id: Long, isRecurring: Boolean)
    suspend fun setCategory(id: Long, categoryId: Long?)
    // Mark/unmark a transaction as a duplicate (also hides it from lists/totals when marked).
    suspend fun setDuplicate(id: Long, isDuplicate: Boolean)
    fun observeDuplicates(): Flow<List<Transaction>>
    suspend fun renameMerchant(oldName: String, newName: String)
    fun getUncategorized(): Flow<List<Transaction>>
    // Splits a parent into child slices: parent is hidden (isIgnored), children inserted with parentId set
    suspend fun splitTransaction(parentId: Long, slices: List<SplitSlice>)
    suspend fun getRecentByAmountAndType(
        amountInPaise: Long,
        type: String,
        since: Long
    ): List<Transaction>
    // Same-day + same amount/type lookup for statement-import Tier 2 dedup (see StatementImportManager).
    suspend fun getByDateRangeAmountAndType(
        startOfDayMillis: Long,
        endOfDayMillis: Long,
        amountInPaise: Long,
        type: String
    ): List<Transaction>
    suspend fun insert(transaction: Transaction): Long
    suspend fun insertParsed(transaction: Transaction, rawText: String?): Long
    suspend fun update(transaction: Transaction)
    suspend fun delete(transaction: Transaction)
    suspend fun deleteById(id: Long)
    suspend fun deleteAll()
}
