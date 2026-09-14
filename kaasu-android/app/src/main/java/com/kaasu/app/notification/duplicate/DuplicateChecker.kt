package com.kaasu.app.notification.duplicate

import com.kaasu.app.core.util.MerchantSimilarity
import com.kaasu.app.domain.repository.TransactionRepository
import com.kaasu.app.notification.model.ParsedTransaction
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuplicateChecker @Inject constructor(
    private val repository: TransactionRepository
) {
    // Called by KaasuNotificationListenerService.process() after TransactionParser; queries TransactionRepository.getRecentByAmountAndType()
    suspend fun isDuplicate(parsed: ParsedTransaction): Boolean {
        val windowStart = parsed.transactionTime - WINDOW_MS
        val recent = repository.getRecentByAmountAndType(
            amountInPaise = parsed.amountInPaise,
            type = parsed.type.name,
            since = windowStart
        )
        return recent.any { existing ->
            // A bank SMS confirmation usually has no merchant, while the UPI app entry is named (or
            // vice-versa, depending on which arrives first). When exactly one side is blank, same
            // amount+type within the window means it's the same payment — drop the duplicate.
            val incomingBlank = parsed.merchantName.isNullOrBlank()
            val existingBlank = existing.merchantName.isNullOrBlank()
            if (incomingBlank != existingBlank) return@any true
            MerchantSimilarity.areSimilar(existing.merchantName, parsed.merchantName)
        }
    }

    // Coarse-precision variant for the accessibility screen-scrape channel only (see
    // TransactionCapturePipeline.process's useCoarseDedup). A history row's scraped timestamp is
    // date-only (DateTextResolver anchors it at noon, no real time-of-day), so the tight WINDOW_MS
    // match above would almost never line up with the real notification/SMS-captured timestamp for
    // the same payment. Widens the match to a full calendar-day range instead, reusing the exact
    // same amount+type+merchant-similarity rule — same shape as StatementImportManager's Tier 2
    // same-day dedup, which solves the identical "coarse timestamp, need a day-wide match" problem.
    /**
     * How many stored transactions already share this amount, direction and calendar day.
     *
     * A count rather than a yes/no, because the screen-scrape channel needs to know *how many* of
     * the rows it can see are already accounted for. Names are deliberately not compared: each
     * channel names the same payment differently — a bank SMS names the account holder it paid
     * ("DHIVYA BHANU S"), Google Pay names the shop ("KOORAI KADAI BIRYANI") — so requiring a
     * merchant match stored both and doubled the spend.
     */
    suspend fun countCoarseMatches(parsed: ParsedTransaction): Int {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(parsed.transactionTime).atZone(zone).toLocalDate()
        val startOfDay = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val candidates = repository.getByDateRangeAmountAndType(
            startOfDayMillis = startOfDay,
            endOfDayMillis = endOfDay,
            amountInPaise = parsed.amountInPaise,
            type = parsed.type.name
        )
        return candidates.size
    }

    companion object {
        // Bank SMS echoes of a UPI payment can lag the app notification by a few minutes, so the
        // window is wider than the payment apps' own grouped re-posts.
        const val WINDOW_MS = 5 * 60 * 1000L
    }
}
