package com.kaasu.app.capture

import com.kaasu.app.BuildConfig
import com.kaasu.app.core.bank.BankRegistry
import com.kaasu.app.core.database.dao.AppSourceDao
import com.kaasu.app.core.database.dao.MerchantAliasDao
import com.kaasu.app.core.util.Hashing
import com.kaasu.app.domain.model.Account
import com.kaasu.app.domain.model.AccountType
import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.domain.repository.AccountRepository
import com.kaasu.app.domain.repository.TransactionRepository
import com.kaasu.app.notification.classifier.CategoryRuleEngine
import com.kaasu.app.notification.duplicate.DuplicateChecker
import com.kaasu.app.notification.model.ParsedTransaction
import com.kaasu.app.notification.model.RawNotification
import com.kaasu.app.notification.parser.AccountNotificationParser
import com.kaasu.app.notification.parser.TransactionParser
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Post-filter capture pipeline shared by every capture channel (notification listener, SMS
 * receiver, SMS backfill worker). Extracted from [com.kaasu.app.notification.listener.KaasuNotificationListenerService]
 * so the same orchestration (parse → alias rename → dedup → classify → account resolution →
 * insert) runs regardless of where the [RawNotification] came from.
 *
 * Callers are expected to have already run their own source-specific filter (NotificationFilter /
 * SmsFilter) — this class starts right after that gate, at TransactionParser.
 */
@Singleton
class TransactionCapturePipeline @Inject constructor(
    private val transactionParser: TransactionParser,
    private val merchantAliasDao: MerchantAliasDao,
    private val duplicateChecker: DuplicateChecker,
    private val appSourceDao: AppSourceDao,
    private val categoryRuleEngine: CategoryRuleEngine,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) {
    // Full pipeline: TransactionParser → DuplicateChecker → CategoryRuleEngine → TransactionRepository.insertParsed
    // Returns true if a transaction was inserted, false if dropped (unparseable or a duplicate).
    //
    // [useCoarseDedup] selects DuplicateChecker.countCoarseMatches (full calendar-day window) instead
    // of the default tight-window isDuplicate. Only the accessibility screen-scrape channel passes
    // true — its scraped timestamps are date-only (see DateTextResolver's noon anchor), so the tight
    // WINDOW_MS match notification/SMS live capture rely on would almost never line up. That channel
    // also only ever inserts transactions no other channel caught (see DuplicateChecker), so a coarse
    // hit here means "drop it," never "merge/update."
    suspend fun process(
        raw: RawNotification,
        useCoarseDedup: Boolean = false,
        merchantOverride: String? = null,
        noteOverride: String? = null,
        coarseBudget: MutableMap<String, Int>? = null,
    ): Boolean {
        var parsed = transactionParser.parse(raw) ?: return false

        // The screen-scrape channel reads the merchant straight off the row, so re-deriving it from
        // a rebuilt sentence would only lose detail — MerchantParser caps a name at three words,
        // which would clip "JAWAHAR NAGAR 70 FEET RD" to "JAWAHAR NAGAR 70".
        merchantOverride?.trim()?.takeIf { it.isNotEmpty() }?.let { parsed = parsed.copy(merchantName = it) }
        // Same reasoning as the merchant: the row states the note outright, so re-deriving it from
        // the sentence with NoteParser would only be a worse guess.
        noteOverride?.trim()?.takeIf { it.isNotEmpty() }?.let { parsed = parsed.copy(note = it) }

        // Apply a user-set merchant rename so this and future captures store the preferred name.
        parsed.merchantName?.let { parsedMerchant ->
            merchantAliasDao.getDisplayName(parsedMerchant.trim().lowercase())?.let { display ->
                parsed = parsed.copy(merchantName = display)
            }
        }

        val isDuplicate = if (useCoarseDedup) {
            isCoveredByExistingRows(parsed, coarseBudget)
        } else {
            duplicateChecker.isDuplicate(parsed)
        }
        if (isDuplicate) return false

        appSourceDao.updateLastSeen(raw.packageName, System.currentTimeMillis())
        val appName = appSourceDao.getByPackage(raw.packageName)?.appName

        // Explicit rules win. When none match, reuse whatever category this merchant was last
        // filed under — so categorising one "SWIGGY" transaction by hand teaches every later one.
        val categoryId = categoryRuleEngine.classify(parsed.merchantName, raw.packageName)
            ?: parsed.merchantName?.let { transactionRepository.getLearnedCategoryIdByMerchant(it) }

        val accountId = resolveAccountId(parsed, appName, accountRepository, transactionRepository)

        // A move between the owner's own accounts arrives as two unrelated messages — the sending
        // bank's debit and the receiving bank's credit — and was stored as an expense plus an
        // income, inflating both. Link them into one labelled transfer.
        val transferLabel = linkInternalTransfer(parsed, accountId)

        val now = System.currentTimeMillis()
        val transaction = parsed.toTransaction(
            appName = appName,
            rawTextHash = Hashing.sha256Prefix(parsed.rawText),
            categoryId = categoryId,
            accountId = accountId,
            now = now
        ).let { tx ->
            if (transferLabel != null) {
                tx.copy(type = TransactionType.TRANSFER, isTransfer = true, merchantName = transferLabel)
            } else {
                tx
            }
        }

        if (BuildConfig.ENABLE_PARSER_LOGS) {
            android.util.Log.d(
                TAG,
                "Parsed: amount=${parsed.amountInPaise} type=${parsed.type} " +
                "confidence=${parsed.confidenceScore} categoryId=$categoryId"
            )
        }

        transactionRepository.insertParsed(transaction, rawText = parsed.rawText)
        return true
    }

    /**
     * Coarse dedup for the screen-scrape channel, counted rather than boolean.
     *
     * A plain "does a same-amount, same-day row exist?" test collapses two genuinely separate
     * payments of the same amount on one day into one, which on a real device rejected every row
     * the screen showed. Instead each existing row absorbs exactly one scraped row: if the day
     * already holds two ₹20 expenses and the screen shows three, the third is new and is inserted.
     *
     * [coarseBudget] carries that tally across one scrape pass; without it this degrades to the
     * old all-or-nothing behaviour, which is the right default for a single stray call.
     */
    private suspend fun isCoveredByExistingRows(
        parsed: ParsedTransaction,
        budget: MutableMap<String, Int>?,
    ): Boolean {
        val key = "${parsed.amountInPaise}|${parsed.type.name}|" +
            java.time.Instant.ofEpochMilli(parsed.transactionTime)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()

        if (budget == null) return duplicateChecker.countCoarseMatches(parsed) > 0

        val remaining = budget.getOrPut(key) { duplicateChecker.countCoarseMatches(parsed) }
        if (BuildConfig.ENABLE_PARSER_LOGS) {
            android.util.Log.d(TAG, "coarse key=$key remaining=$remaining merchant=${parsed.merchantName}")
        }
        if (remaining <= 0) return false
        budget[key] = remaining - 1
        return true
    }

    /**
     * Detects a move between two of the owner's own accounts and labels both legs.
     *
     * Each bank announces only its own half: IDFC says "A/c XX3956 debited … hakeema188@okaxis
     * credited", Union Bank says "A/c *0913 Credited for Rs:10.00". Nothing connects them, so the
     * pair was stored as a ₹10 expense and a ₹10 income — both totals wrong, and neither row said
     * where the money actually went.
     *
     * The signature of a transfer is a debit and a credit of the identical amount, landing on two
     * different accounts of the owner's within [TRANSFER_WINDOW_MS]. When the second leg arrives,
     * the first is rewritten too, so both read "IDFC FIRST → Union Bank".
     *
     * Returns that label for the incoming transaction, or null when this is an ordinary payment.
     */
    private suspend fun linkInternalTransfer(parsed: ParsedTransaction, accountId: Long?): String? {
        if (accountId == null) return null
        val counterpartType = when (parsed.type) {
            TransactionType.EXPENSE -> TransactionType.INCOME
            TransactionType.INCOME -> TransactionType.EXPENSE
            else -> return null
        }

        val counterpart = transactionRepository.getRecentByAmountAndType(
            amountInPaise = parsed.amountInPaise,
            type = counterpartType.name,
            since = parsed.transactionTime - TRANSFER_WINDOW_MS,
        ).firstOrNull { it.accountId != null && it.accountId != accountId && !it.isTransfer }
            ?: return null

        val names = accountRepository.getAll().first().associate { it.id to it.displayName }
        val thisName = names[accountId] ?: return null
        val otherName = names[counterpart.accountId] ?: return null

        // Direction reads from the money's point of view: whichever leg is the debit is the "from".
        val label = if (parsed.type == TransactionType.EXPENSE) {
            "$thisName → $otherName"
        } else {
            "$otherName → $thisName"
        }

        transactionRepository.update(
            counterpart.copy(
                type = TransactionType.TRANSFER,
                isTransfer = true,
                merchantName = label,
                updatedAt = System.currentTimeMillis(),
            )
        )
        return label
    }

    // Resolves which account a captured transaction belongs to, auto-creating one when a new
    // card/account tail is seen. Falls back to "same merchant → same account" memory.
    private suspend fun resolveAccountId(
        parsed: ParsedTransaction,
        appName: String?,
        accountRepo: AccountRepository,
        txRepo: TransactionRepository
    ): Long? {
        val lastFour = AccountNotificationParser.extractLastFour(parsed.rawText)
        if (lastFour != null) {
            val matches = accountRepo.getByLastFour(lastFour)
            return when {
                matches.size == 1 -> matches.first().id
                matches.isEmpty() -> {
                    // First time we see this tail — create the account so it shows up for manual use
                    val issuer = AccountNotificationParser.extractIssuer(parsed.rawText)
                    val isCard = AccountNotificationParser.isCreditCard(parsed.rawText)
                    // The SMS sender header ("sms:JM-IDFCFB-S") names the bank even when the
                    // message body does not, which is how accounts were ending up called "Bank".
                    val base = issuer ?: appName?.takeIf { it.isNotBlank() }
                        ?: parsed.sourceAppName?.takeIf { it.isNotBlank() }
                        ?: BankRegistry.resolve(parsed.sourceAppPackage)?.displayName
                        ?: "Bank"
                    accountRepo.insert(
                        Account(
                            displayName = if (isCard) "$base Card" else base,
                            lastFourDigits = lastFour,
                            accountType = if (isCard) AccountType.CREDIT_CARD else AccountType.SAVINGS,
                            colorArgb = ACCOUNT_COLORS[abs(lastFour.hashCode()) % ACCOUNT_COLORS.size],
                            isActive = true,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
                else -> null // same tail across multiple banks — ambiguous, leave unlinked
            }
        }
        // No card/account tail (e.g. a UPI "paid to X") — reuse the account last used for this merchant
        return parsed.merchantName?.let { txRepo.getLatestAccountIdByMerchant(it) }
    }

    companion object {
        private const val TAG = "KaasuCapturePipeline"

        /** Two banks announcing the same move rarely land more than a few minutes apart. */
        private const val TRANSFER_WINDOW_MS = 15 * 60 * 1000L

        // Palette for auto-created accounts (deterministic per tail so colors stay stable).
        private val ACCOUNT_COLORS = listOf(
            0xFF0F4A37.toInt(), 0xFF1F8458.toInt(), 0xFF1D4A7A.toInt(),
            0xFFA36B00.toInt(), 0xFF8E24AA.toInt(), 0xFFB54040.toInt(),
        )
    }
}

private fun ParsedTransaction.toTransaction(
    appName: String?,
    rawTextHash: String,
    categoryId: Long?,
    accountId: Long?,
    now: Long
): Transaction = Transaction(
    amountInPaise = amountInPaise,
    currency = currency,
    type = type,
    merchantName = merchantName,
    categoryId = categoryId,
    sourceAppPackage = sourceAppPackage,
    sourceAppName = appName ?: sourceAppName,
    paymentMode = null,
    rawTextHash = rawTextHash,
    confidenceScore = confidenceScore,
    transactionTime = transactionTime,
    createdAt = now,
    updatedAt = now,
    isManual = false,
    isTransfer = type.name == "TRANSFER",
    isRefund = type.name == "REFUND" || type.name == "CASHBACK",
    isIgnored = false,
    note = note,
    accountId = accountId,
    isRecurring = isRecurring
)
