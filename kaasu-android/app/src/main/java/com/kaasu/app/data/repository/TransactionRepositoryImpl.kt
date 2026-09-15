package com.kaasu.app.data.repository

import com.kaasu.app.core.database.dao.TransactionDao
import com.kaasu.app.data.mapper.toDomain
import com.kaasu.app.data.mapper.toEntity
import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.repository.SplitSlice
import com.kaasu.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao
) : TransactionRepository {

    override fun getAll(): Flow<List<Transaction>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> =
        dao.getByDateRange(startTime, endTime).map { list -> list.map { it.toDomain() } }

    override fun getByCategory(categoryId: Long): Flow<List<Transaction>> =
        dao.getByCategory(categoryId).map { list -> list.map { it.toDomain() } }

    override fun search(query: String): Flow<List<Transaction>> =
        dao.search(query).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Transaction? =
        dao.getById(id)?.toDomain()

    override fun observeById(id: Long): Flow<Transaction?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getByHash(hash: String): Transaction? =
        dao.getByHash(hash)?.toDomain()

    override suspend fun findByRawTextContaining(pattern: String): List<Transaction> =
        dao.findByRawTextContaining(pattern).map { it.toDomain() }

    override suspend fun getRawText(id: Long): String? =
        dao.getRawTextById(id)

    override suspend fun getLatestAccountIdByMerchant(merchant: String): Long? =
        dao.getLatestAccountIdByMerchant(merchant)

    override suspend fun getLearnedCategoryIdByMerchant(merchant: String): Long? =
        dao.getLearnedCategoryIdByMerchant(merchant)

    override suspend fun setRecurring(id: Long, isRecurring: Boolean) =
        dao.setRecurring(id, isRecurring, System.currentTimeMillis())

    override suspend fun setCategory(id: Long, categoryId: Long?) =
        dao.setCategory(id, categoryId, System.currentTimeMillis())

    override suspend fun setDuplicate(id: Long, isDuplicate: Boolean) =
        dao.setDuplicate(id, isDuplicate, System.currentTimeMillis())

    override fun observeDuplicates(): Flow<List<Transaction>> =
        dao.observeDuplicates().map { list -> list.map { it.toDomain() } }

    override suspend fun renameMerchant(oldName: String, newName: String) =
        dao.renameMerchant(oldName, newName, System.currentTimeMillis())

    override fun getUncategorized(): Flow<List<Transaction>> =
        dao.getUncategorized().map { list -> list.map { it.toDomain() } }

    override suspend fun splitTransaction(parentId: Long, slices: List<SplitSlice>) {
        val parent = dao.getById(parentId) ?: return
        val now = System.currentTimeMillis()
        // Each slice inherits the parent's merchant/account/time/type so it reads naturally in lists.
        val children = slices.map { slice ->
            parent.copy(
                id = 0,
                amountInPaise = slice.amountInPaise,
                categoryId = slice.categoryId,
                parentId = parentId,
                isIgnored = false,
                isManual = true,
                rawText = null,
                rawTextHash = null,
                createdAt = now,
                updatedAt = now
            )
        }
        dao.splitTransaction(parentId, children, now)
    }

    override suspend fun getRecentByAmountAndType(
        amountInPaise: Long,
        type: String,
        since: Long
    ): List<Transaction> =
        dao.getRecentByAmountAndType(amountInPaise, type, since).map { it.toDomain() }

    override suspend fun getByDateRangeAmountAndType(
        startOfDayMillis: Long,
        endOfDayMillis: Long,
        amountInPaise: Long,
        type: String
    ): List<Transaction> =
        dao.getByDateRangeAmountAndType(startOfDayMillis, endOfDayMillis, amountInPaise, type)
            .map { it.toDomain() }

    override suspend fun insert(transaction: Transaction): Long =
        dao.insert(transaction.toEntity())

    override suspend fun insertParsed(transaction: Transaction, rawText: String?): Long =
        dao.insert(transaction.toEntity(rawText = rawText))

    override suspend fun update(transaction: Transaction) =
        dao.update(transaction.toEntity())

    override suspend fun delete(transaction: Transaction) =
        dao.delete(transaction.toEntity())

    override suspend fun deleteById(id: Long) =
        dao.deleteById(id)



    override suspend fun getByTransferGroup(groupId: String): List<Transaction> =
        dao.getByTransferGroup(groupId).map { it.toDomain() }

    override fun observeHalfLinkedTransfers(): Flow<List<Transaction>> =
        dao.observeHalfLinkedTransfers().map { list -> list.map { it.toDomain() } }

    override suspend fun deleteByTransferGroup(groupId: String) = dao.deleteByTransferGroup(groupId)

    override suspend fun deleteAll() = dao.deleteAll()
}
