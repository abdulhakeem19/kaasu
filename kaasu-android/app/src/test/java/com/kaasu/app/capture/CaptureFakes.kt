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
import com.kaasu.app.domain.repository.AccountRepository
import com.kaasu.app.domain.repository.SplitSlice
import com.kaasu.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory stand-ins for the storage and DAO layers, so capture tests exercise the real pipeline,
 * parser, dedup and rule engine against everything else.
 *
 * Shared rather than nested inside one test: a second copy is how two tests end up disagreeing
 * about what the storage layer does, and then agreeing with each other instead of with production.
 */

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

    fun all(): List<Account> = accounts.toList()

    fun nameOf(id: Long?): String? = accounts.firstOrNull { it.id == id }?.displayName

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
    override suspend fun update(account: Account) {
        val i = accounts.indexOfFirst { it.id == account.id }
        if (i >= 0) accounts[i] = account
    }

    override suspend fun setLastStatedBalance(id: Long, balanceInPaise: Long, at: Long) {
        val i = accounts.indexOfFirst { it.id == id }
        if (i >= 0) accounts[i] = accounts[i].copy(
            lastStatedBalanceInPaise = balanceInPaise,
            lastStatedBalanceAt = at,
        )
    }

    fun statedBalanceOf(id: Long): Long? =
        accounts.firstOrNull { it.id == id }?.lastStatedBalanceInPaise
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
