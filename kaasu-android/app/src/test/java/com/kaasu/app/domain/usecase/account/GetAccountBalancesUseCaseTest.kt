package com.kaasu.app.domain.usecase.account

import com.kaasu.app.domain.model.Account
import com.kaasu.app.domain.model.AccountType
import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.domain.model.TransferRole
import com.kaasu.app.domain.repository.AccountRepository
import com.kaasu.app.domain.repository.SplitSlice
import com.kaasu.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetAccountBalancesUseCaseTest {

    private val day = 1_756_684_800_000L // 1 Sep 2025

    private fun account(
        id: Long,
        name: String,
        type: AccountType = AccountType.SAVINGS,
        opening: Long? = 0L,
        openingAt: Long? = day,
        stated: Long? = null,
        statedAt: Long? = null,
    ) = Account(
        id = id, displayName = name, lastFourDigits = null, accountType = type,
        colorArgb = null, isActive = true, createdAt = 0L,
        openingBalanceInPaise = opening, openingBalanceAt = openingAt,
        lastStatedBalanceInPaise = stated, lastStatedBalanceAt = statedAt,
    )

    private fun tx(
        type: TransactionType,
        amountInPaise: Long,
        accountId: Long?,
        at: Long = day + 1_000L,
        transferRole: TransferRole? = null,
    ) = Transaction(
        id = 0, amountInPaise = amountInPaise, currency = "INR", type = type,
        merchantName = null, categoryId = null, sourceAppPackage = null, sourceAppName = null,
        paymentMode = null, rawTextHash = null, confidenceScore = 100, transactionTime = at,
        createdAt = 0L, updatedAt = 0L, isManual = false,
        isTransfer = type == TransactionType.TRANSFER, isRefund = false, isIgnored = false,
        note = null, accountId = accountId, isRecurring = false, parentId = null,
        isDuplicate = false, transferGroupId = null, transferRole = transferRole,
        counterpartAccountId = null,
    )

    private fun useCase(accounts: List<Account>, transactions: List<Transaction>) =
        GetAccountBalancesUseCase(FakeAccounts(accounts), FakeTransactions(transactions))

    // ── Derived balance ───────────────────────────────────────────────────────

    @Test fun `balance is the opening figure plus everything since`() = runTest {
        val balances = useCase(
            accounts = listOf(account(1, "Union Bank", opening = 1_000_000L)),
            transactions = listOf(
                tx(TransactionType.EXPENSE, 250_000L, 1L),
                tx(TransactionType.INCOME, 50_000L, 1L),
            ),
        )().first()

        assertEquals(800_000L, balances.single().balanceInPaise)
        assertEquals(2, balances.single().transactionCount)
    }

    @Test fun `transactions before the opening balance are already in it`() = runTest {
        // Counting them again would apply the same money twice.
        val balances = useCase(
            accounts = listOf(account(1, "Union Bank", opening = 1_000_000L, openingAt = day)),
            transactions = listOf(
                tx(TransactionType.EXPENSE, 250_000L, 1L, at = day - 86_400_000L),
                tx(TransactionType.EXPENSE, 100_000L, 1L, at = day + 1_000L),
            ),
        )().first()

        assertEquals(900_000L, balances.single().balanceInPaise)
        assertEquals("only the later row counts", 1, balances.single().transactionCount)
    }

    @Test fun `an account with no opening balance reports an unknown balance`() = runTest {
        // An auto-created account must not claim a confident ₹0 — the UI asks for the real figure.
        val balances = useCase(
            accounts = listOf(account(1, "SBI Card", opening = null, openingAt = null)),
            transactions = listOf(tx(TransactionType.EXPENSE, 239_900L, 1L)),
        )().first()

        assertFalse(balances.single().isKnown)
    }

    // ── Transfers ─────────────────────────────────────────────────────────────

    @Test fun `a transfer moves money between accounts without changing the total`() = runTest {
        val balances = useCase(
            accounts = listOf(
                account(1, "Union Bank", opening = 1_000_000L),
                account(2, "IDFC FIRST", opening = 0L),
            ),
            transactions = listOf(
                tx(TransactionType.TRANSFER, 400_000L, 1L, transferRole = TransferRole.OUT),
                tx(TransactionType.TRANSFER, 400_000L, 2L, transferRole = TransferRole.IN),
            ),
        )().first()

        assertEquals(600_000L, balances.first { it.account?.id == 1L }.balanceInPaise)
        assertEquals(400_000L, balances.first { it.account?.id == 2L }.balanceInPaise)
        assertEquals("the money moved, it did not multiply", 1_000_000L, balances.sumOf { it.balanceInPaise })
    }

    @Test fun `a card reads as an amount outstanding rather than a negative balance`() = runTest {
        val balances = useCase(
            accounts = listOf(account(9, "SBI Card", type = AccountType.CREDIT_CARD, opening = 0L)),
            transactions = listOf(tx(TransactionType.EXPENSE, 239_900L, 9L)),
        )().first()

        val card = balances.single()
        assertTrue(card.isCreditCard)
        assertEquals(-239_900L, card.balanceInPaise)
        assertEquals(239_900L, card.outstandingInPaise)
    }

    @Test fun `paying the bill settles the card`() = runTest {
        val balances = useCase(
            accounts = listOf(account(9, "SBI Card", type = AccountType.CREDIT_CARD, opening = 0L)),
            transactions = listOf(
                tx(TransactionType.EXPENSE, 239_900L, 9L),
                tx(TransactionType.TRANSFER, 239_900L, 9L, transferRole = TransferRole.IN),
            ),
        )().first()

        assertEquals(0L, balances.single().outstandingInPaise)
    }

    // ── Money with nowhere to go ──────────────────────────────────────────────

    @Test fun `transactions on no account get a bucket of their own`() = runTest {
        // Without one they vanish from the account view, which reads as having less than you do.
        val balances = useCase(
            accounts = listOf(account(1, "Union Bank", opening = 1_000_000L)),
            transactions = listOf(
                tx(TransactionType.EXPENSE, 100_000L, 1L),
                tx(TransactionType.EXPENSE, 30_000L, null),
            ),
        )().first()

        val unassigned = balances.single { it.isUnassigned }
        assertEquals(-30_000L, unassigned.balanceInPaise)
        assertEquals(1, unassigned.transactionCount)
        assertFalse("it is a prompt, not an account", unassigned.isKnown)
    }

    @Test fun `no bucket appears when every transaction has an account`() = runTest {
        val balances = useCase(
            accounts = listOf(account(1, "Union Bank")),
            transactions = listOf(tx(TransactionType.EXPENSE, 100_000L, 1L)),
        )().first()

        assertTrue(balances.none { it.isUnassigned })
    }

    // ── Reconciliation ────────────────────────────────────────────────────────

    @Test fun `drift is the gap between our figure and the bank's`() = runTest {
        val balances = useCase(
            accounts = listOf(account(1, "Union Bank", opening = 1_243_000L, stated = 1_198_000L, statedAt = day)),
            transactions = emptyList(),
        )().first()

        assertEquals(45_000L, balances.single().driftInPaise)
    }

    @Test fun `no drift to report when the bank has said nothing`() = runTest {
        val balances = useCase(
            accounts = listOf(account(1, "Union Bank", opening = 1_243_000L)),
            transactions = emptyList(),
        )().first()

        assertNull(balances.single().driftInPaise)
    }

    @Test fun `no drift to report while the balance itself is unknown`() = runTest {
        // Comparing a figure we do not have against the bank's would be meaningless.
        val balances = useCase(
            accounts = listOf(account(1, "Union Bank", opening = null, openingAt = null, stated = 1_198_000L)),
            transactions = emptyList(),
        )().first()

        assertNull(balances.single().driftInPaise)
    }

    // ── Fakes ─────────────────────────────────────────────────────────────────

    private class FakeAccounts(private val accounts: List<Account>) : AccountRepository {
        override fun getAll(): Flow<List<Account>> = flowOf(accounts)
        override suspend fun getById(id: Long) = accounts.firstOrNull { it.id == id }
        override suspend fun getByLastFour(lastFour: String) = emptyList<Account>()
        override suspend fun insert(account: Account): Long = 1L
        override suspend fun update(account: Account) {}
        override suspend fun delete(id: Long) {}
        override suspend fun setLastStatedBalance(id: Long, balanceInPaise: Long, at: Long) {}
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
