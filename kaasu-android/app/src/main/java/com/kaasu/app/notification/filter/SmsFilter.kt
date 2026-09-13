package com.kaasu.app.notification.filter

import com.kaasu.app.core.database.dao.IgnoredPatternDao
import com.kaasu.app.core.database.dao.SmsSenderDao
import com.kaasu.app.core.database.entity.SmsSenderEntity
import com.kaasu.app.notification.model.RawNotification
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gate for raw SMS capture (live [com.kaasu.app.sms.receiver.SmsReceiver] and the historical
 * [com.kaasu.app.sms.worker.SmsBackfillWorker] backfill). Unlike [NotificationFilter], SMS has no
 * package name to allowlist against — Indian DLT sender-ID formats vary too much by telecom
 * circle to be a reliable hard gate, and legit bank headers are reused for promotional traffic
 * too. So content ([FinancialTextHeuristics]) is the mandatory gate here; sender format is only
 * ever a soft confidence nudge applied later in [com.kaasu.app.notification.parser.TransactionParser].
 */
@Singleton
class SmsFilter @Inject constructor(
    private val smsSenderDao: SmsSenderDao,
    private val ignoredPatternDao: IgnoredPatternDao,
) {
    @Volatile private var cachedIgnoredPatterns: List<String>? = null

    suspend fun shouldProcess(raw: RawNotification): Boolean {
        val text = raw.fullText()
        if (text.isBlank()) return false

        // Mandatory content gate — sender format alone never lets a message through.
        if (!FinancialTextHeuristics.looksFinancial(text)) return false

        val senderId = raw.packageName.removePrefix(SMS_PACKAGE_PREFIX)
        val existing = smsSenderDao.getBySenderId(senderId)
        if (existing == null) {
            // First time we see this sender — auto-create the registry row (mirrors how Accounts
            // are auto-created on first-seen card tail), defaulting enabled so capture starts
            // immediately; the user can disable it later from SMS sources.
            val now = System.currentTimeMillis()
            smsSenderDao.insert(
                SmsSenderEntity(
                    senderId = senderId,
                    displayLabel = null,
                    isEnabled = true,
                    isKnownFinanceSender = false,
                    lastSeenAt = now,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            if (!existing.isEnabled) return false
            smsSenderDao.updateLastSeen(senderId, System.currentTimeMillis())
        }

        val patterns = cachedIgnoredPatterns ?: ignoredPatternDao.getAllPatterns()
            .also { cachedIgnoredPatterns = it }
        val lower = text.lowercase()
        if (patterns.any { lower.contains(it.lowercase()) }) return false

        return true
    }

    // Call after inserting a new IgnoredPattern row so the in-memory set refreshes
    fun invalidateCache() {
        cachedIgnoredPatterns = null
    }

    companion object {
        const val SMS_PACKAGE_PREFIX = "sms:"
    }
}
