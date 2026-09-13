package com.kaasu.app.accessibility.scraper

import javax.inject.Inject

/**
 * PhonePe transaction-history screen scraper. `isTransactionScreen` matches defensively on
 * resource-id SUBSTRINGS rather than exact ids — PhonePe's exact view ids churn across app version
 * bumps, and a substring match survives most of that churn.
 */
class PhonePeScreenScraper @Inject constructor() : BaseTransactionScreenScraper() {
    override val packageName: String = "com.phonepe.app"
    override val screenResourceIdHints = listOf("transaction", "txn", "history", "amount")
}
