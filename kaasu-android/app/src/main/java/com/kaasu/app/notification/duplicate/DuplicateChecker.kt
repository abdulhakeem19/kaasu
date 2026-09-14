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
    suspend fun isDuplicateCoarse(parsed: ParsedTransaction): Boolean {
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
        // Deliberately does NOT compare merchant names, unlike the tight-window check.
        //
        // The same payment is named differently by different channels: a bank SMS names the
        // account holder it paid ("DHIVYA BHANU S") while Google Pay names the shop
        // ("KOORAI KADAI BIRYANI"). MerchantSimilarity rightly says those are unrelated, so
        // requiring a merchant match let both be stored and doubled the spend.
        //
        // Same amount, same direction, same calendar day is accepted as the same payment here.
        // That can discard a genuinely separate second payment of the identical amount on the
        // same day — but this channel exists only to add what no other channel caught, so missing
        // one is the right side to err on. Double-counting silently inflates every total.
        return candidates.isNotEmpty()
    }

    companion object {
        // Bank SMS echoes of a UPI payment can lag the app notification by a few minutes, so the
        // window is wider than the payment apps' own grouped re-posts.
        const val WINDOW_MS = 5 * 60 * 1000L
    }
}
