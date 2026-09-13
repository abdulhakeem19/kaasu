package com.kaasu.app.notification.duplicate

import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.domain.repository.TransactionRepository
import com.kaasu.app.notification.model.ParsedTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DuplicateCheckerTest {

    private lateinit var checker: DuplicateChecker
    private lateinit var fakeRepo: FakeTransactionRepository

    @Before
    fun setUp() {
        fakeRepo = FakeTransactionRepository()
        checker = DuplicateChecker(fakeRepo)
    }

    private fun parsed(
        amountInPaise: Long = 50000L,
        type: TransactionType = TransactionType.EXPENSE,
        merchant: String? = "Swiggy",
        time: Long = 1000L
    ) = ParsedTransaction(
        amountInPaise = amountInPaise,
        type = type,
        merchantName = merchant,
        sourceAppPackage = "com.google.android.apps.nbu.paisa.user",
        sourceAppName = "Google Pay",
        rawText = "₹500 paid to Swiggy",
        confidenceScore = 85,
        transactionTime = time
    )

    private fun existing(
        amountInPaise: Long = 50000L,
        type: TransactionType = TransactionType.EXPENSE,
        merchant: String? = "Swiggy",
        time: Long = 500L
    ) = Transaction(
        id = 1L,
        amountInPaise = amountInPaise,
        type = type,
        merchantName = merchant,
        categoryId = null,
        sourceAppPackage = "com.google.android.apps.nbu.paisa.user",
        sourceAppName = "Google Pay",
        paymentMode = null,
        rawTextHash = "abc123",
        transactionTime = time,
        createdAt = time,
        updatedAt = time,
        note = null
    )

    @Test fun isDuplicate_whenSameAmountTypeAndMerchantWithin2Min() = runTest {
        fakeRepo.recentResults = listOf(existing())
        assertTrue(checker.isDuplicate(parsed()))
    }

    @Test fun notDuplicate_whenNoRecentTransactions() = runTest {
        fakeRepo.recentResults = emptyList()
        assertFalse(checker.isDuplicate(parsed()))
    }

    @Test fun notDuplicate_whenMerchantDiffers() = runTest {
        fakeRepo.recentResults = listOf(existing(merchant = "Zomato"))
        assertFalse(checker.isDuplicate(parsed(merchant = "Swiggy")))
    }

    @Test fun isDuplicate_whenBothMerchantsAreNull() = runTest {
        fakeRepo.recentResults = listOf(existing(merchant = null))
        assertTrue(checker.isDuplicate(parsed(merchant = null)))
    }

    // Existing=null (bank SMS recorded first), incoming has merchant (UPI app) → same payment, duplicate.
    // Bank-echo matching is symmetric regardless of which notification arrives first.
    @Test fun isDuplicate_whenExistingNullAndIncomingHasMerchant() = runTest {
        fakeRepo.recentResults = listOf(existing(merchant = null))
        assertTrue(checker.isDuplicate(parsed(merchant = "Swiggy")))
    }

    // Incoming=null (bank SMS confirmation), existing has merchant (UPI app recorded it first) → bank is duplicate
    @Test fun isDuplicate_whenIncomingNullAndExistingHasMerchant() = runTest {
        fakeRepo.recentResults = listOf(existing(merchant = "MEENAKSHI"))
        assertTrue(checker.isDuplicate(parsed(merchant = null)))
    }

    @Test fun isDuplicate_caseInsensitiveMerchant() = runTest {
        fakeRepo.recentResults = listOf(existing(merchant = "SWIGGY"))
        assertTrue(checker.isDuplicate(parsed(merchant = "swiggy")))
    }

    @Test fun isDuplicate_partialMerchantMatch() = runTest {
        fakeRepo.recentResults = listOf(existing(merchant = "Swiggy India"))
        assertTrue(checker.isDuplicate(parsed(merchant = "Swiggy")))
    }

    // ── isDuplicateCoarse — accessibility screen-scrape channel ─────────────────
    // A scraped candidate's transactionTime is date-only (DateTextResolver anchors it at noon),
    // so it will almost never fall inside isDuplicate's tight WINDOW_MS — these exercise the
    // widened full-calendar-day variant instead, via getByDateRangeAmountAndType.

    @Test fun isDuplicateCoarse_whenSameDayAmountTypeAndMerchant() = runTest {
        fakeRepo.dateRangeResults = listOf(existing(time = 500L))
        assertTrue(checker.isDuplicateCoarse(parsed(time = 1000L)))
    }

    @Test fun notDuplicateCoarse_whenNoSameDayTransactions() = runTest {
        fakeRepo.dateRangeResults = emptyList()
        assertFalse(checker.isDuplicateCoarse(parsed()))
    }

    @Test fun notDuplicateCoarse_whenMerchantDiffers() = runTest {
        fakeRepo.dateRangeResults = listOf(existing(merchant = "Zomato"))
        assertFalse(checker.isDuplicateCoarse(parsed(merchant = "Swiggy")))
    }

    @Test fun isDuplicateCoarse_caseInsensitivePartialMerchantMatch() = runTest {
        fakeRepo.dateRangeResults = listOf(existing(merchant = "SWIGGY INDIA"))
        assertTrue(checker.isDuplicateCoarse(parsed(merchant = "swiggy")))
    }

    @Test fun isDuplicateCoarse_whenBothMerchantsAreNull() = runTest {
        fakeRepo.dateRangeResults = listOf(existing(merchant = null))
        assertTrue(checker.isDuplicateCoarse(parsed(merchant = null)))
    }

    @Test fun notDuplicateCoarse_ignoresIsDuplicateOnlyFakeData() = runTest {
        // isDuplicateCoarse must query the date-range lookup, not the tight-window one.
        fakeRepo.recentResults = listOf(existing())
        fakeRepo.dateRangeResults = emptyList()
        assertFalse(checker.isDuplicateCoarse(parsed()))
    }

    // ── FakeTransactionRepository ─────────────────────────────────────────────

    class FakeTransactionRepository : TransactionRepository {
        var recentResults: List<Transaction> = emptyList()
        var dateRangeResults: List<Transaction> = emptyList()

        override suspend fun getRecentByAmountAndType(
            amountInPaise: Long,
            type: String,
            since: Long
        ) = recentResults

        override suspend fun getByDateRangeAmountAndType(
            startOfDayMillis: Long,
            endOfDayMillis: Long,
            amountInPaise: Long,
            type: String
        ): List<Transaction> = dateRangeResults

        override fun getAll(): Flow<List<Transaction>> = throw UnsupportedOperationException()
        override fun getByDateRange(s: Long, e: Long): Flow<List<Transaction>> = throw UnsupportedOperationException()
        override fun getByCategory(id: Long): Flow<List<Transaction>> = throw UnsupportedOperationException()
        override fun search(q: String): Flow<List<Transaction>> = throw UnsupportedOperationException()
        override suspend fun getById(id: Long): Transaction? = null
        override fun observeById(id: Long): Flow<Transaction?> = throw UnsupportedOperationException()
        override suspend fun getByHash(hash: String): Transaction? = null
        override suspend fun findByRawTextContaining(pattern: String): List<Transaction> = emptyList()
        override suspend fun getRawText(id: Long): String? = null
        override suspend fun getLatestAccountIdByMerchant(merchant: String): Long? = null
        override suspend fun getLearnedCategoryIdByMerchant(merchant: String): Long? = null
        override suspend fun setRecurring(id: Long, isRecurring: Boolean) {}
        override suspend fun setCategory(id: Long, categoryId: Long?) {}
        override suspend fun setDuplicate(id: Long, isDuplicate: Boolean) {}
        override fun observeDuplicates(): Flow<List<Transaction>> = throw UnsupportedOperationException()
        override suspend fun renameMerchant(oldName: String, newName: String) {}
        override fun getUncategorized(): Flow<List<Transaction>> = throw UnsupportedOperationException()
        override suspend fun splitTransaction(parentId: Long, slices: List<com.kaasu.app.domain.repository.SplitSlice>) {}
        override suspend fun insert(t: Transaction): Long = 1L
        override suspend fun insertParsed(t: Transaction, rawText: String?): Long = 1L
        override suspend fun update(t: Transaction) = Unit
        override suspend fun delete(t: Transaction) = Unit
        override suspend fun deleteById(id: Long) = Unit
        override suspend fun getTotalSpendInPaise(s: Long, e: Long): Long = 0L
        override suspend fun getTotalIncomeInPaise(s: Long, e: Long): Long = 0L
        override suspend fun deleteAll() = Unit
    }
}
