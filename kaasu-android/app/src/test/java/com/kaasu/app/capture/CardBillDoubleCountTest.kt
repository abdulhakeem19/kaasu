package com.kaasu.app.capture

import com.kaasu.app.core.database.dao.AppSourceDao
import com.kaasu.app.core.database.dao.MerchantAliasDao
import com.kaasu.app.core.database.dao.RuleDao
import com.kaasu.app.core.database.entity.AppSourceEntity
import com.kaasu.app.core.database.entity.MerchantAliasEntity
import com.kaasu.app.core.database.entity.RuleEntity
import com.kaasu.app.domain.model.Account
import com.kaasu.app.domain.model.AccountType
import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.domain.model.TransferRole
import com.kaasu.app.domain.money.SpendRules
import com.kaasu.app.domain.repository.AccountRepository
import com.kaasu.app.domain.repository.SplitSlice
import com.kaasu.app.domain.repository.TransactionRepository
import com.kaasu.app.notification.classifier.CategoryRuleEngine
import com.kaasu.app.notification.duplicate.DuplicateChecker
import com.kaasu.app.notification.model.RawNotification
import com.kaasu.app.notification.parser.TransactionParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The bug that started all of this, driven end to end through the real pipeline.
 *
 * A ₹2,399 subscription auto-debited on the SBI credit card, then ₹2,399 moved from savings to
 * clear that card. The money left once. Kaasu reported ₹4,798.
 *
 * Everything here is the production code except the storage and DAO layers.
 */
class CardBillDoubleCountTest {

    private lateinit var repo: RecordingTransactionRepository
    private lateinit var accounts: FakeAccountRepository
    private lateinit var rules: FakeRuleDao
    private lateinit var pipeline: TransactionCapturePipeline

    private var savingsId = 0L
    private var cardId = 0L

    private val day = 1_756_684_800_000L // 1 Sep 2025, UTC

    @Before
    fun setUp() {
        repo = RecordingTransactionRepository()
        accounts = FakeAccountRepository()
        rules = FakeRuleDao()
        savingsId = accounts.seed("Union Bank", "5678", AccountType.SAVINGS)
        cardId = accounts.seed("SBI Card", "1234", AccountType.CREDIT_CARD)
        pipeline = TransactionCapturePipeline(
            transactionParser = TransactionParser(),
            merchantAliasDao = FakeMerchantAliasDao(),
            duplicateChecker = DuplicateChecker(repo),
            appSourceDao = FakeAppSourceDao(),
            categoryRuleEngine = CategoryRuleEngine(rules),
            accountRepository = accounts,
            transactionRepository = repo,
        )
    }

    private suspend fun capture(text: String, at: Long = day) =
        pipeline.process(
            RawNotification(
                packageName = "com.sbi.upi",
                appName = "SBI",
                title = null,
                text = text,
                subText = null,
                postedAt = at,
            )
        )

    private val cardPurchase =
        "Rs.2399.00 spent on your SBI Credit Card ending 1234 at CLAUDE AI on 01-Sep-25."

    private val billPayment =
        "Rs.2399.00 debited from A/c XX5678 towards your credit card bill payment " +
            "for card ending 1234. Thank you."

    // ── The headline case ─────────────────────────────────────────────────────

    @Test
    fun `the card purchase and clearing that card count the money once`() = runTest {
        assertTrue(capture(cardPurchase))
        assertTrue(capture(billPayment, at = day + 60_000))

        assertEquals(2, repo.stored.size)
        assertEquals(
            "one ₹2,399 subscription, paid once",
            239_900L,
            SpendRules.netSpendInPaise(repo.stored),
        )
    }

    @Test
    fun `clearing the card is recorded as a transfer into the card, not a purchase`() = runTest {
        capture(cardPurchase)
        capture(billPayment, at = day + 60_000)

        val bill = repo.stored.last()
        assertEquals(TransactionType.TRANSFER, bill.type)
        assertEquals(TransferRole.OUT, bill.transferRole)
        assertEquals("paid out of the savings account", savingsId, bill.accountId)
        assertEquals("and into the card", cardId, bill.counterpartAccountId)
        assertNotNull(bill.transferGroupId)
        assertNull("a transfer is not spending, so it carries no category", bill.categoryId)
    }

    @Test
    fun `the purchase on the card stays a real expense`() = runTest {
        capture(cardPurchase)
        val purchase = repo.stored.single()
        assertEquals(TransactionType.EXPENSE, purchase.type)
        assertEquals(cardId, purchase.accountId)
        assertTrue(SpendRules.isSpend(purchase))
    }

    // ── Negative control: the fix must not erase real spending ────────────────

    @Test
    fun `the same amount paid to an ordinary payee is still spending`() = runTest {
        assertTrue(capture("Rs.2399.00 debited from A/c XX5678 to ARUN STORES via UPI."))
        val tx = repo.stored.single()
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertNull(tx.transferGroupId)
        assertEquals(239_900L, SpendRules.netSpendInPaise(repo.stored))
    }

    @Test
    fun `a bill-shaped message is left alone when no card is tracked`() = runTest {
        // Guessing here would silently erase a genuine expense from the month.
        accounts.removeCreditCards()
        assertTrue(capture(billPayment))
        assertEquals(TransactionType.EXPENSE, repo.stored.single().type)
    }

    // ── Bank to bank ──────────────────────────────────────────────────────────

    @Test
    fun `a move between two of the owner's accounts groups both legs and is not spending`() = runTest {
        accounts.removeCreditCards()
        val idfcId = accounts.seed("IDFC FIRST", "3956", AccountType.SAVINGS)

        assertTrue(capture("Rs.10.00 debited from A/c XX3956 to hakeema188@okaxis via UPI."))
        assertTrue(capture("A/c XX5678 Credited for Rs:10.00 by UPI.", at = day + 30_000))

        assertEquals(2, repo.stored.size)
        val out = repo.stored.first { it.transferRole == TransferRole.OUT }
        val into = repo.stored.first { it.transferRole == TransferRole.IN }

        assertEquals("both legs belong to one movement", out.transferGroupId, into.transferGroupId)
        assertEquals(idfcId, out.accountId)
        assertEquals(savingsId, into.accountId)
        assertEquals(savingsId, out.counterpartAccountId)
        assertEquals(idfcId, into.counterpartAccountId)
        assertEquals("moving your own money is not spending", 0L, SpendRules.netSpendInPaise(repo.stored))
    }

    @Test
    fun `the arrow is no longer written into the merchant name`() = runTest {
        accounts.removeCreditCards()
        accounts.seed("IDFC FIRST", "3956", AccountType.SAVINGS)
        capture("Rs.10.00 debited from A/c XX3956 to hakeema188@okaxis via UPI.")
        capture("A/c XX5678 Credited for Rs:10.00 by UPI.", at = day + 30_000)

        // The direction lives in the account ids now, so it can be queried and survives a rename.
        assertTrue(repo.stored.none { it.merchantName?.contains("→") == true })
    }

    // ── A rule can declare the type outright ──────────────────────────────────

    @Test
    fun `a type-override rule turns a matching payment into a transfer`() = runTest {
        accounts.removeCreditCards()
        rules.rules = listOf(
            RuleEntity(
                id = 1, name = "Credit Card Bill", matchText = "credit card",
                matchType = "CONTAINS", categoryId = null, transactionType = "TRANSFER",
                sourceAppPackage = null, priority = 20, isSystem = true, isActive = true,
                createdAt = 0L, updatedAt = 0L,
            )
        )
        assertTrue(capture("Rs.2399.00 debited from A/c XX5678 at HDFC CREDIT CARD on 01-Sep-25."))

        val tx = repo.stored.single()
        assertEquals(TransactionType.TRANSFER, tx.type)
        assertNull(tx.categoryId)
        assertEquals(0L, SpendRules.netSpendInPaise(repo.stored))
    }

    @Test
    fun `a rule with no type override still only sets the category`() = runTest {
        rules.rules = listOf(
            RuleEntity(
                id = 1, name = "Swiggy", matchText = "swiggy",
                matchType = "CONTAINS", categoryId = 7L, transactionType = null,
                sourceAppPackage = null, priority = 10, isSystem = true, isActive = true,
                createdAt = 0L, updatedAt = 0L,
            )
        )
        capture("Rs.300.00 debited from A/c XX5678 at SWIGGY on 01-Sep-25.")
        val tx = repo.stored.single()
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(7L, tx.categoryId)
    }

    // ── Fakes ─────────────────────────────────────────────────────────────────

    class RecordingTransactionRepository : TransactionRepository {
        val stored = mutableListOf<Transaction>()
        private var nextId = 1L

        override suspend fun insertParsed(t: Transaction, rawText: String?): Long {
            val id = nextId++
            stored += t.copy(id = id)
            return id
        }

        override suspend fun insert(t: Transaction): Long = insertParsed(t, null)

        override suspend fun update(t: Transaction) {
            val i = stored.indexOfFirst { it.id == t.id }
            if (i >= 0) stored[i] = t
        }

        override suspend fun getRecentByAmountAndType(amountInPaise: Long, type: String, since: Long) =
            stored.filter { it.amountInPaise == amountInPaise && it.type.name == type && it.transactionTime >= since }

        override suspend fun getByDateRangeAmountAndType(
            startOfDayMillis: Long, endOfDayMillis: Long, amountInPaise: Long, type: String,
        ): List<Transaction> = stored.filter {
            it.amountInPaise == amountInPaise && it.type.name == type &&
                it.transactionTime in startOfDayMillis..endOfDayMillis
        }

        override suspend fun getByTransferGroup(groupId: String) =
            stored.filter { it.transferGroupId == groupId }

        override fun observeHalfLinkedTransfers(): Flow<List<Transaction>> = flowOf(emptyList())

        override suspend fun deleteByTransferGroup(groupId: String) {
            stored.removeAll { it.transferGroupId == groupId }
        }

        override fun getAll(): Flow<List<Transaction>> = flowOf(stored)
        override fun getByDateRange(s: Long, e: Long): Flow<List<Transaction>> = flowOf(stored)
        override fun getByCategory(id: Long): Flow<List<Transaction>> = flowOf(emptyList())
        override fun search(q: String): Flow<List<Transaction>> = flowOf(emptyList())
        override suspend fun getById(id: Long): Transaction? = stored.firstOrNull { it.id == id }
        override fun observeById(id: Long): Flow<Transaction?> = flowOf(null)
        override suspend fun getByHash(hash: String): Transaction? = null
        override suspend fun findByRawTextContaining(pattern: String): List<Transaction> = emptyList()
        override suspend fun getRawText(id: Long): String? = null
        override suspend fun getLatestAccountIdByMerchant(merchant: String): Long? = null
        override suspend fun getLearnedCategoryIdByMerchant(merchant: String): Long? = null
        override suspend fun setRecurring(id: Long, isRecurring: Boolean) {}
        override suspend fun setCategory(id: Long, categoryId: Long?) {}
        override suspend fun setDuplicate(id: Long, isDuplicate: Boolean) {}
        override fun observeDuplicates(): Flow<List<Transaction>> = flowOf(emptyList())
        override suspend fun renameMerchant(oldName: String, newName: String) {}
        override fun getUncategorized(): Flow<List<Transaction>> = flowOf(emptyList())
        override suspend fun splitTransaction(parentId: Long, slices: List<SplitSlice>) {}
        override suspend fun delete(t: Transaction) { stored.remove(t) }
        override suspend fun deleteById(id: Long) { stored.removeAll { it.id == id } }
        override suspend fun deleteAll() { stored.clear() }
    }

    class FakeAccountRepository : AccountRepository {
        private val accounts = mutableListOf<Account>()
        private var nextId = 1L

        fun seed(name: String, lastFour: String, type: AccountType): Long {
            val id = nextId++
            accounts += Account(
                id = id, displayName = name, lastFourDigits = lastFour, accountType = type,
                colorArgb = null, isActive = true, createdAt = 0L,
            )
            return id
        }

        fun removeCreditCards() {
            accounts.removeAll { it.accountType == AccountType.CREDIT_CARD }
        }

        override fun getAll(): Flow<List<Account>> = flowOf(accounts.toList())
        override suspend fun getById(id: Long) = accounts.firstOrNull { it.id == id }
        override suspend fun getByLastFour(lastFour: String) =
            accounts.filter { it.lastFourDigits == lastFour && it.isActive }
        override suspend fun insert(account: Account): Long {
            val id = nextId++
            accounts += account.copy(id = id)
            return id
        }
        override suspend fun update(account: Account) {}
        override suspend fun delete(id: Long) { accounts.removeAll { it.id == id } }
    }

    class FakeRuleDao : RuleDao {
        var rules: List<RuleEntity> = emptyList()
        override suspend fun getActiveRulesList(): List<RuleEntity> = rules
        override suspend fun insert(rule: RuleEntity): Long = 1L
        override suspend fun update(rule: RuleEntity) {}
        override suspend fun delete(id: Long) {}
        override fun getAll(): Flow<List<RuleEntity>> = flowOf(rules)
        override fun getActiveByPriority(): Flow<List<RuleEntity>> = flowOf(rules)
        override suspend fun getById(id: Long): RuleEntity? = null
        override suspend fun deleteUserRules() {}
        override suspend fun deleteAllRules() {}
        override suspend fun getAllForBackup(): List<RuleEntity> = rules
        override suspend fun insertAll(rules: List<RuleEntity>) {}
    }

    class FakeMerchantAliasDao : MerchantAliasDao {
        override suspend fun upsert(alias: MerchantAliasEntity) {}
        override suspend fun getDisplayName(originalLower: String): String? = null
        override suspend fun getAll(): List<MerchantAliasEntity> = emptyList()
        override suspend fun deleteAll() {}
    }

    class FakeAppSourceDao : AppSourceDao {
        override suspend fun insert(appSource: AppSourceEntity): Long = 1L
        override suspend fun update(appSource: AppSourceEntity) {}
        override fun getAll(): Flow<List<AppSourceEntity>> = flowOf(emptyList())
        override suspend fun getByPackage(packageName: String): AppSourceEntity? = null
        override suspend fun getKnownFinanceApps(): List<AppSourceEntity> = emptyList()
        override suspend fun updateLastSeen(packageName: String, time: Long) {}
        override suspend fun updateLastAccessibilityScrapeAttempt(packageName: String, time: Long) {}
        override suspend fun updateLastAccessibilityScrapeSuccess(packageName: String, time: Long) {}
    }
}
