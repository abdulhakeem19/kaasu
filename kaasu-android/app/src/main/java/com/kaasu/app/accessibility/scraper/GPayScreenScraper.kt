package com.kaasu.app.accessibility.scraper

import javax.inject.Inject

/**
 * Google Pay transaction-history scraper. All of the work lives in [BaseTransactionScreenScraper] —
 * this exists only to bind a package name, since detection is now based on the shape of the
 * on-screen content rather than per-app view ids.
 */
class GPayScreenScraper @Inject constructor() : BaseTransactionScreenScraper() {
    override val packageName: String = "com.google.android.apps.nbu.paisa.user"
}
