package com.kaasu.app.accessibility.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.kaasu.app.BuildConfig
import com.kaasu.app.accessibility.model.toRawNotification
import com.kaasu.app.accessibility.scraper.ScraperRegistry
import com.kaasu.app.capture.TransactionCapturePipeline
import com.kaasu.app.core.database.dao.AppSourceDao
import com.kaasu.app.di.ApplicationScope
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Fourth transaction-capture channel: opportunistically reads UPI apps' own on-screen
 * transaction-history text (see [ScraperRegistry]) to catch payments none of the other three
 * channels (notification listener, SMS, statement import) ever recorded. Purely passive — it never
 * calls `performGlobalAction`/`performAction` to open, navigate, or tap anything; it only reacts to
 * screen content the owner already put there by using the target app themselves.
 *
 * **Two-layer scope filter** (this is the entire safety model for the most powerful permission
 * class on Android — never widen either layer casually):
 *  (a) static — `res/xml/accessibility_service_config.xml` sets `android:packageNames` to
 *      [com.kaasu.app.notification.filter.SourceApps.PAYMENT_APP_PACKAGES] and
 *      `android:accessibilityEventTypes` to ONLY `typeWindowStateChanged|typeWindowContentChanged`
 *      (never a click/text-changed/all-mask type — this app must never observe PIN-entry
 *      keystrokes or arbitrary taps). Android itself pre-filters almost everything before it
 *      reaches this process.
 *  (b) dynamic — [ScraperRegistry.resolve] narrows further to only packages that actually have a
 *      registered [com.kaasu.app.accessibility.scraper.ScreenScraper] (today: GPay, PhonePe).
 *
 * [onAccessibilityEvent] additionally guards, redundantly, against Kaasu's own package and wraps
 * the entire scrape-and-capture attempt in `runCatching` — a crash here can disable the ENTIRE
 * system accessibility service until the user manually re-enables it in Settings, so a bug in one
 * scraper must never be allowed to propagate out of this callback.
 */
class KaasuAccessibilityService : AccessibilityService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AccessibilityServiceEntryPoint {
        fun scraperRegistry(): ScraperRegistry
        fun transactionCapturePipeline(): TransactionCapturePipeline
        fun appSourceDao(): AppSourceDao
        @ApplicationScope fun applicationScope(): CoroutineScope
    }

    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            AccessibilityServiceEntryPoint::class.java
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return

        // Redundant safety net on top of the static XML package allow-list: never process our own
        // package's events, even if the config were ever accidentally widened.
        if (pkg == packageName) return

        runCatching {
            handleEvent(pkg)
        }.onFailure { e ->
            if (BuildConfig.ENABLE_PARSER_LOGS) {
                Log.w(TAG, "Accessibility scrape failed for $pkg: ${e.message}")
            }
        }
    }

    private fun handleEvent(pkg: String) {
        val ep = entryPoint
        val scraper = ep.scraperRegistry().resolve(pkg) ?: return
        val root = rootInActiveWindow ?: run {
            if (BuildConfig.ENABLE_PARSER_LOGS) Log.d(TAG, "$pkg: no root window")
            return
        }
        try {
            // This channel's failure mode is silence — it shipped for weeks capturing nothing with
            // nothing to show why. Debug-only breadcrumbs at each early return make that visible.
            // ENABLE_PARSER_LOGS is false in release, so no captured text is ever logged there.
            if (!scraper.isTransactionScreen(root)) {
                if (BuildConfig.ENABLE_PARSER_LOGS) Log.d(TAG, "$pkg: not a transaction screen")
                return
            }
            val candidates = scraper.extractCandidates(root)
            if (BuildConfig.ENABLE_PARSER_LOGS) Log.d(TAG, "$pkg: ${candidates.size} candidate rows")
            if (candidates.isEmpty()) return

            ep.applicationScope().launch {
                // A single screen fires several typeWindowContentChanged events in a row, and each
                // one used to start its own insert pass before any had committed — so DuplicateChecker
                // saw nothing and the same three rows were stored three times. The mutex serialises
                // the passes; the signature skips a screen whose rows have not changed since the last.
                scrapeMutex.withLock {
                val signature = candidates.joinToString("|") { it.rawNodeText }
                if (signature == lastScrapeSignature) return@withLock
                lastScrapeSignature = signature

                val now = System.currentTimeMillis()
                ep.appSourceDao().updateLastAccessibilityScrapeAttempt(pkg, now)

                var anyInserted = false
                for (candidate in candidates) {
                    val raw = candidate.toRawNotification()
                    // Coarse dedup: this channel only ever inserts what no other channel caught —
                    // see TransactionCapturePipeline.process's useCoarseDedup and DuplicateChecker.
                    if (ep.transactionCapturePipeline().process(
                            raw,
                            useCoarseDedup = true,
                            merchantOverride = candidate.merchantText,
                    )) {
                        anyInserted = true
                    }
                }

                if (BuildConfig.ENABLE_PARSER_LOGS) {
                    Log.d(TAG, "$pkg: inserted=$anyInserted from ${candidates.size} candidates")
                }
                if (anyInserted) {
                    ep.appSourceDao().updateLastAccessibilityScrapeSuccess(pkg, System.currentTimeMillis())
                }
                }
            }
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }

    private val scrapeMutex = Mutex()

    /** Rows of the screen last processed, so an unchanged screen is not re-inserted. */
    @Volatile
    private var lastScrapeSignature: String? = null

    override fun onInterrupt() = Unit

    private companion object {
        const val TAG = "KaasuAccessibility"
    }
}
