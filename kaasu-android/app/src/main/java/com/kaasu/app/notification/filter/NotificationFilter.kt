package com.kaasu.app.notification.filter

import com.kaasu.app.core.database.dao.AppSourceDao
import com.kaasu.app.core.database.dao.IgnoredPatternDao
import com.kaasu.app.notification.model.RawNotification
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationFilter @Inject constructor(
    private val appSourceDao: AppSourceDao,
    private val ignoredPatternDao: IgnoredPatternDao,
    /**
     * Whether direct SMS capture is active. Injected rather than read from a Context here so this
     * filter stays a plain testable class — the Android lookup lives in its Hilt binding.
     */
    private val smsCapture: SmsCaptureAvailability,
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

        // With READ_SMS granted, SmsReceiver already captures every bank SMS directly, and the
        // messaging app's notification about that same SMS is a second copy of it. The two are not
        // recognised as duplicates: the notification text carries the sender header ("AD-IDFCFB-S
        // Your A/c…") so its hash differs, and the notification can arrive hours later — well past
        // DuplicateChecker's window — so both were stored and the spend was counted twice.
        //
        // Observed on a real device: one ₹150 payment stored at 09:59 from sms:AD-IDFCFB-S and
        // again at 15:00 from com.google.android.apps.messaging.
        if (isMessagingApp && !isPaymentApp) {
            if (smsCapture.isGranted()) return false
            // Without SMS access the notification is the only way this transaction is seen, so it
            // stays — but messaging apps carry everything (OTPs, chats, promos), hence the filter.
            if (!FinancialTextHeuristics.looksFinancial(text)) return false
        }

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
