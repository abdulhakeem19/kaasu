package com.kaasu.app.notification.filter

import com.kaasu.app.core.database.dao.AppSourceDao
import com.kaasu.app.core.database.dao.IgnoredPatternDao
import com.kaasu.app.notification.model.RawNotification
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationFilter @Inject constructor(
    private val appSourceDao: AppSourceDao,
    private val ignoredPatternDao: IgnoredPatternDao
) {
    @Volatile private var cachedFinancePackages: Set<String>? = null
    @Volatile private var cachedIgnoredPatterns: List<String>? = null

    // Called by KaasuNotificationListenerService.process() — first gate in the pipeline; returns false to abort early
    suspend fun shouldProcess(notification: RawNotification): Boolean {
        val packages = cachedFinancePackages ?: appSourceDao.getKnownFinanceApps()
            .map { it.packageName }.toSet().also { cachedFinancePackages = it }

        val patterns = cachedIgnoredPatterns ?: ignoredPatternDao.getAllPatterns()
            .also { cachedIgnoredPatterns = it }

        val text = notification.fullText()
        if (text.isBlank()) return false

        val pkg = notification.packageName
        // Strict allowlist: a payment/bank app (built-in list ∪ user-added bank sources) or the
        // system messaging app. Anything else is dropped regardless of content.
        val isPaymentApp = pkg in SourceApps.PAYMENT_APP_PACKAGES || pkg in packages
        val isMessagingApp = pkg in SourceApps.MESSAGING_APP_PACKAGES
        if (!isPaymentApp && !isMessagingApp) return false

        // Messaging apps carry everything (OTPs, chats, promos via SMS) — only let through SMS that
        // actually look like a bank transaction. Payment apps skip this; their marketing is handled
        // downstream by PromotionalDetector in the parser.
        if (isMessagingApp && !isPaymentApp && !FinancialTextHeuristics.looksFinancial(text)) return false

        val lower = text.lowercase()
        if (patterns.any { lower.contains(it.lowercase()) }) return false

        return true
    }

    // Call after inserting a new AppSource or IgnoredPattern row so the in-memory set refreshes
    fun invalidateCache() {
        cachedFinancePackages = null
        cachedIgnoredPatterns = null
    }
}
