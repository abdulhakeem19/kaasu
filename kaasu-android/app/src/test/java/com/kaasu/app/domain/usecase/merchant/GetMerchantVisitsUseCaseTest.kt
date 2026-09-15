package com.kaasu.app.domain.usecase.merchant

import com.kaasu.app.core.database.dao.MerchantAliasDao
import com.kaasu.app.core.database.entity.MerchantAliasEntity
import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.domain.model.TransferRole
import com.kaasu.app.domain.repository.SplitSlice
import com.kaasu.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetMerchantVisitsUseCaseTest {

    private val day = 1_756_684_800_000L // 1 Sep 2025
    private val windowStart = day
    private val windowEnd = day + 29 * 86_400_000L

    private fun tx(
        merchant: String?,
        amountInPaise: Long = 30_000L,
        at: Long = day + 3_600_000L,
        type: TransactionType = TransactionType.EXPENSE,
        isIgnored: Boolean = false,
        isDuplicate: Boolean = false,
        transferRole: TransferRole? = null,
    ) = Transaction(
        id = 0, amountInPaise = amountInPaise, currency = "INR", type = type,
        merchantName = merchant, categoryId = null, sourceAppPackage = null, sourceAppName = null,
        paymentMode = null, rawTextHash = null, confidenceScore = 100, transactionTime = at,
        createdAt = 0L, updatedAt = 0L, isManual = false,
        isTransfer = type == TransactionType.TRANSFER, isRefund = false, isIgnored = isIgnored,
        note = null, accountId = 1L, isRecurring = false, parentId = null,
        isDuplicate = isDuplicate, transferGroupId = null, transferRole = transferRole,
        counterpartAccountId = null,
    )

    private fun useCase(
        transactions: List<Transaction>,
        aliases: List<MerchantAliasEntity> = emptyList(),
    ) = GetMerchantVisitsUseCase(FakeTransactions(transactions), FakeAliasDao(aliases))

    @Test fun `counts every visit to the same merchant`() = runTest {
        val visits = useCase(List(8) { tx("SWIGGY", 40_500L) })("SWIGGY", windowStart, windowEnd)!!
        assertEquals(8, visits.totalVisits)
        assertEquals(324_000L, visits.totalSpentInPaise)
        assertTrue(visits.isRepeat)
    }

    @Test fun `variants of the same name count together`() = runTest {
        // The same rule the dedup paths already use, so "Swiggy Instamart" is not a new merchant.
        val visits = useCase(listOf(
            tx("SWIGGY"), tx("Swiggy Instamart"), tx("swiggy"),
        ))("SWIGGY", windowStart, windowEnd)!!
        assertEquals(3, visits.totalVisits)
    }

    @Test fun `a renamed merchant keeps its history`() = runTest {
        // Tidying a name up must not reset the habit to one visit.
        val visits = useCase(
            transactions = listOf(tx("arun stores"), tx("ARUN STORES"), tx("Arun Stores")),
            aliases = listOf(MerchantAliasEntity(
                originalLower = "arun stores", displayName = "Arun Stores",
                createdAt = 0L, updatedAt = 0L,
            )),
        )("Arun Stores", windowStart, windowEnd)!!
        assertEquals(3, visits.totalVisits)
    }

    @Test fun `the window figures are separate from the all-time ones`() = runTest {
        val visits = useCase(listOf(
            tx("SWIGGY", 30_000L, at = day + 3_600_000L),
            tx("SWIGGY", 30_000L, at = day + 3_600_000L),
            tx("SWIGGY", 50_000L, at = day - 60L * 86_400_000L), // months earlier
        ))("SWIGGY", windowStart, windowEnd)!!

        assertEquals(3, visits.totalVisits)
        assertEquals(110_000L, visits.totalSpentInPaise)
        assertEquals(2, visits.visitsInWindow)
        assertEquals(60_000L, visits.spentInWindowInPaise)
    }

    @Test fun `only real spending counts as a visit`() = runTest {
        // A transfer is not a purchase, and ignored or duplicate rows would inflate the habit.
        val visits = useCase(listOf(
            tx("SWIGGY"),
            tx("SWIGGY", isIgnored = true),
            tx("SWIGGY", isDuplicate = true),
            tx("SWIGGY", type = TransactionType.TRANSFER, transferRole = TransferRole.OUT),
        ))("SWIGGY", windowStart, windowEnd)!!
        assertEquals(1, visits.totalVisits)
    }

    @Test fun `a one-off is not a repeat`() = runTest {
        val visits = useCase(listOf(tx("ARUN STORES")))("ARUN STORES", windowStart, windowEnd)!!
        assertEquals(1, visits.totalVisits)
        assertFalse(visits.isRepeat)
    }

    @Test fun `the average is per visit`() = runTest {
        val visits = useCase(listOf(
            tx("SWIGGY", 20_000L), tx("SWIGGY", 40_000L),
        ))("SWIGGY", windowStart, windowEnd)!!
        assertEquals(30_000L, visits.averageSpendInPaise)
    }

    @Test fun `first and last visit are reported`() = runTest {
        val visits = useCase(listOf(
            tx("SWIGGY", at = day + 1_000L), tx("SWIGGY", at = day + 9_000L),
        ))("SWIGGY", windowStart, windowEnd)!!
        assertEquals(day + 1_000L, visits.firstVisitAt)
        assertEquals(day + 9_000L, visits.lastVisitAt)
    }

    @Test fun `an unknown merchant reports nothing`() = runTest {
        assertNull(useCase(listOf(tx("SWIGGY")))("ZOMATO", windowStart, windowEnd))
    }

    @Test fun `a blank or missing name reports nothing`() = runTest {
        // Rows with no merchant would otherwise all collapse into one imaginary shop.
        assertNull(useCase(listOf(tx(null)))(null, windowStart, windowEnd))
        assertNull(useCase(listOf(tx("SWIGGY")))("   ", windowStart, windowEnd))
    }

    // ── Fakes ─────────────────────────────────────────────────────────────────

    private class FakeAliasDao(private val aliases: List<MerchantAliasEntity>) : MerchantAliasDao {
        override suspend fun upsert(alias: MerchantAliasEntity) {}
        override suspend fun getDisplayName(originalLower: String): String? = null
        override suspend fun getAll(): List<MerchantAliasEntity> = aliases
        override suspend fun deleteAll() {}
    }

    private class FakeTransactions(private val transactions: List<Transaction>) : TransactionRepository {
        override fun getAll(): Flow<List<Transaction>> = flowOf(transactions)
        override fun getByDateRange(s: Long, e: Long): Flow<List<Transaction>> = flowOf(transactions)
        override fun getByCategory(id: Long): Flow<List<Transaction>> = flowOf(emptyList())
        override fun search(q: String): Flow<List<Transaction>> = flowOf(emptyList())
        override suspend fun getById(id: Long): Transaction? = null
        override fun observeById(id: Long): Flow<Transaction?> = flowOf(null)
        override suspend fun getByHash(hash: String): Transaction? = null
        override suspend fun findByRawTextContaining(pattern: String) = emptyList<Transaction>()
        override suspend fun getRawText(id: Long): String? = null
        override suspend fun getLatestAccountIdByMerchant(merchant: String): Long? = null
        override suspend fun getLearnedCategoryIdByMerchant(merchant: String): Long? = null
        override suspend fun getRecentByAmountAndType(amountInPaise: Long, type: String, since: Long) = emptyList<Transaction>()
        override suspend fun getByDateRangeAmountAndType(
            startOfDayMillis: Long, endOfDayMillis: Long, amountInPaise: Long, type: String,
        ) = emptyList<Transaction>()
        override suspend fun setRecurring(id: Long, isRecurring: Boolean) {}
        override suspend fun setCategory(id: Long, categoryId: Long?) {}
        override suspend fun setDuplicate(id: Long, isDuplicate: Boolean) {}
        override fun observeDuplicates(): Flow<List<Transaction>> = flowOf(emptyList())
        override suspend fun renameMerchant(oldName: String, newName: String) {}
        override fun getUncategorized(): Flow<List<Transaction>> = flowOf(emptyList())
        override suspend fun splitTransaction(parentId: Long, slices: List<SplitSlice>) {}
        override suspend fun insert(t: Transaction): Long = 1L
        override suspend fun insertParsed(t: Transaction, rawText: String?): Long = 1L
        override suspend fun update(t: Transaction) {}
        override suspend fun delete(t: Transaction) {}
        override suspend fun deleteById(id: Long) {}
        override suspend fun getByTransferGroup(groupId: String) = emptyList<Transaction>()
        override fun observeHalfLinkedTransfers(): Flow<List<Transaction>> = flowOf(emptyList())
        override suspend fun deleteByTransferGroup(groupId: String) {}
        override suspend fun deleteAll() {}
    }
}
