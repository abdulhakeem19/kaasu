package com.kaasu.app.accessibility.model

import com.kaasu.app.accessibility.extraction.DateTextResolver
import com.kaasu.app.notification.model.RawNotification

/**
 * One candidate transaction row reconstructed from on-screen text fragments read off a UPI app's
 * transaction-history screen by a [com.kaasu.app.accessibility.scraper.ScreenScraper]. Deliberately
 * NOT a new sealed capture-source type — see [toRawNotification]: it is wrapped into the exact same
 * [RawNotification] model notification/SMS capture already use, so [com.kaasu.app.notification.parser.TransactionParser]
 * runs unmodified for this fourth channel too.
 */
data class ScrapedTransactionCandidate(
    val amountText: String?,
    val merchantText: String?,
    val dateText: String?,
    val directionHint: String?,
    val rawNodeText: String,
    val sourcePackage: String,
    val scrapedAt: Long = System.currentTimeMillis()
)

// A history row's date is typically bare ("28 Aug", "Yesterday") with no time-of-day — resolve it
// via DateTextResolver (noon anchor when no time fragment is present) rather than trusting scrapedAt
// (the moment Kaasu happened to observe the row on screen, which can be days after the real payment
// if the owner scrolls back through old history).
fun ScrapedTransactionCandidate.toRawNotification(): RawNotification {
    val resolvedTimestamp = DateTextResolver.resolve(dateText, anchorMillis = scrapedAt) ?: scrapedAt
    return RawNotification(
        packageName = sourcePackage,
        appName = null,
        title = null,
        text = rawNodeText,
        subText = null,
        postedAt = resolvedTimestamp
    )
}
