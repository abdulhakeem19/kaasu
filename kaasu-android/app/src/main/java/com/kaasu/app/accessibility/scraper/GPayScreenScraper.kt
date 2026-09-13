package com.kaasu.app.accessibility.scraper

import javax.inject.Inject

/**
 * Google Pay transaction-history screen scraper. `isTransactionScreen` matches defensively on
 * resource-id SUBSTRINGS rather than exact ids — GPay's exact view ids churn across app version
 * bumps, and a substring match survives most of that churn.
 */
class GPayScreenScraper @Inject constructor() : BaseTransactionScreenScraper() {
    override val packageName: String = "com.google.android.apps.nbu.paisa.user"
    override val screenResourceIdHints = listOf("transaction", "txn", "history", "amount")
}
