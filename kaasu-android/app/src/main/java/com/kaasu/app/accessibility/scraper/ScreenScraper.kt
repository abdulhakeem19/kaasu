package com.kaasu.app.accessibility.scraper

import android.view.accessibility.AccessibilityNodeInfo
import com.kaasu.app.accessibility.model.ScrapedTransactionCandidate

/**
 * Per-app screen scraper for [com.kaasu.app.accessibility.service.KaasuAccessibilityService].
 * Implementations are looked up by package name via [ScraperRegistry] — being on
 * [com.kaasu.app.notification.filter.SourceApps.PAYMENT_APP_PACKAGES] (the static XML config's
 * package allow-list) does not imply a scraper exists; only apps with a registered [ScreenScraper]
 * are ever actually walked.
 */
interface ScreenScraper {
    val packageName: String

    /** Cheap check: does this window look like a transaction-history screen worth walking? */
    fun isTransactionScreen(root: AccessibilityNodeInfo): Boolean

    /** Walks [root] and reconstructs zero or more transaction candidates from it. */
    fun extractCandidates(root: AccessibilityNodeInfo): List<ScrapedTransactionCandidate>
}
