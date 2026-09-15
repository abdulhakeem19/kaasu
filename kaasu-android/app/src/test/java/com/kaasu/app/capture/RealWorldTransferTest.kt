package com.kaasu.app.capture

import com.kaasu.app.domain.model.AccountType
import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.domain.model.TransferRole
import com.kaasu.app.domain.money.SpendRules
import com.kaasu.app.domain.money.TransferLabel
import com.kaasu.app.notification.classifier.CategoryRuleEngine
import com.kaasu.app.notification.duplicate.DuplicateChecker
import com.kaasu.app.notification.model.RawNotification
import com.kaasu.app.notification.parser.AccountNotificationParser
import com.kaasu.app.notification.parser.TransactionParser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * A real ₹10 transfer between the owner's own banks, as his two banks actually announced it.
 *
 * Kaasu missed this while axio caught it, and the cause was one regex. IDFC masks its account as
 * "A/C XXXXX103956", where the account is ...3956; the old pattern took the *first* four digits
 * after the mask, read "1039", failed its word-boundary check and fell through every fallback to
 * null. A credit with no account cannot be paired with its debit, so `linkInternalTransfer`
 * returned at its first line and the money was filed as unexplained income.
 *
 * Message text is the genuine wording with the name, phone numbers and reference number replaced —
 * the masking style and layout are what matter here, and real bank messages must not enter the
 * repository.
 */
class RealWorldTransferTest {

    private lateinit var repo: RecordingTransactionRepository
    private lateinit var accounts: FakeAccountRepository
    private lateinit var pipeline: TransactionCapturePipeline

    private var unionId = 0L

    /** 15 Sep 2026, 11:11 — both messages landed in the same minute. */
    private val at = 1_789_200_660_000L

    @Before
    fun setUp() {
        repo = RecordingTransactionRepository()
        accounts = FakeAccountRepository()
        // Only Union Bank is known up front; IDFC is auto-created from its own message, which is
        // exactly the state his device was in.
        unionId = accounts.seed("Union Bank", "0913", AccountType.SAVINGS)
        pipeline = TransactionCapturePipeline(
            transactionParser = TransactionParser(),
            merchantAliasDao = FakeMerchantAliasDao(),
            duplicateChecker = DuplicateChecker(repo),
            appSourceDao = FakeAppSourceDao(),
            categoryRuleEngine = CategoryRuleEngine(FakeRuleDao()),
            accountRepository = accounts,
            transactionRepository = repo,
        )
    }

    private val unionDebit =
        "Union Bank of India A/c *0913 Debited Rs:10.00 on 15-09-2026 11:11:06 by Mob Bk " +
            "ref no 000000000000, Fvg: Test Na Avl Bal Rs:91.25. " +
            "Not you?Call 18002333/SMS BLOCK 0913 to 0000000000"

    private val idfcCredit =
        "Your A/C XXXXX103956 is credited with INR 10.00 on 15/09/26 11:11. " +
            "Your new balance is INR 2,064.68. Team IDFC FIRST Bank"

    private suspend fun capture(text: String, at: Long) =
        pipeline.process(RawNotification("sms", "SMS", null, text, null, at))

    // ── The account number, which is where it broke ───────────────────────────

    @Test
    fun `IDFC's long mask yields the last four digits, the way the bank's own app shows them`() {
        assertEquals("3956", AccountNotificationParser.extractLastFour(idfcCredit))
    }

    @Test
    fun `Union Bank's short mask still works`() {
        assertEquals("0913", AccountNotificationParser.extractLastFour(unionDebit))
    }

    @Test
    fun `a reference number is not mistaken for an account`() {
        // The debit carries a 12-digit reference; only the masked number is an account.
        assertEquals("0913", AccountNotificationParser.extractLastFour(unionDebit))
    }

    // ── The transfer itself ───────────────────────────────────────────────────

    @Test
    fun `both legs are grouped into one movement`() = runTest {
        assertTrue(capture(unionDebit, at))
        assertTrue(capture(idfcCredit, at + 6_000L))

        assertEquals(2, repo.stored.size)
        val out = repo.stored.first { it.transferRole == TransferRole.OUT }
        val into = repo.stored.first { it.transferRole == TransferRole.IN }

        assertNotNull(out.transferGroupId)
        assertEquals(out.transferGroupId, into.transferGroupId)
        assertEquals("the money left Union Bank", unionId, out.accountId)
        assertEquals("and arrived at IDFC", into.accountId, out.counterpartAccountId)
    }

    @Test
    fun `moving his own money is not spending`() = runTest {
        capture(unionDebit, at)
        capture(idfcCredit, at + 6_000L)

        assertEquals(0L, SpendRules.netSpendInPaise(repo.stored))
        assertEquals("nor is it income", 0L, SpendRules.totalIncomeInPaise(repo.stored))
    }

    @Test
    fun `it reads the way axio put it`() = runTest {
        // axio: "₹10.00 Transferred from Union Bank to IDFC 3956".
        capture(unionDebit, at)
        capture(idfcCredit, at + 6_000L)

        val out = repo.stored.first { it.transferRole == TransferRole.OUT }
        val label = TransferLabel.of(
            out,
            accountName = accounts.nameOf(out.accountId),
            counterpartName = accounts.nameOf(out.counterpartAccountId),
        )
        assertEquals("Union Bank → IDFC FIRST", label)
    }

    @Test
    fun `the credit arriving first works the same`() = runTest {
        // Delivery order is not guaranteed, and whichever leg lands second does the pairing.
        assertTrue(capture(idfcCredit, at))
        assertTrue(capture(unionDebit, at + 6_000L))

        assertEquals(2, repo.stored.size)
        assertTrue(repo.stored.all { it.type == TransactionType.TRANSFER })
        assertEquals(0L, SpendRules.netSpendInPaise(repo.stored))
    }

    @Test
    fun `the IDFC account is created from its own message`() = runTest {
        capture(idfcCredit, at)
        val created = accounts.all().first { it.lastFourDigits == "3956" }
        assertEquals("IDFC FIRST", created.displayName)
        assertEquals("a bank account, not a card", AccountType.SAVINGS, created.accountType)
    }

    // ── The balances both banks volunteered ───────────────────────────────────

    @Test
    fun `each bank's stated balance is recorded against its own account`() = runTest {
        capture(unionDebit, at)
        capture(idfcCredit, at + 6_000L)

        assertEquals("Avl Bal Rs:91.25", 9_125L, accounts.statedBalanceOf(unionId))
        val idfcId = accounts.all().first { it.lastFourDigits == "3956" }.id
        assertEquals("new balance is INR 2,064.68", 206_468L, accounts.statedBalanceOf(idfcId))
    }

    // ── Not every credit is a transfer ────────────────────────────────────────

    @Test
    fun `an unmatched credit stays income`() = runTest {
        // Nothing debited ₹10 nearby, so there is no movement to pair it with.
        assertTrue(capture(idfcCredit, at))
        val only = repo.stored.single()
        assertEquals(TransactionType.INCOME, only.type)
        assertEquals(1_000L, SpendRules.totalIncomeInPaise(repo.stored))
    }
}
