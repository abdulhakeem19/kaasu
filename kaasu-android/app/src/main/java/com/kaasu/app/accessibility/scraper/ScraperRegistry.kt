package com.kaasu.app.accessibility.scraper

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves an accessibility event's source package to its [ScreenScraper], or null. This is the
 * dynamic (b) layer of the two-layer scope filter described on
 * [com.kaasu.app.accessibility.service.KaasuAccessibilityService] — the static XML config's
 * `android:packageNames` already narrows delivered events to the payment-apps allow-list, but being
 * on that list doesn't mean a scraper actually exists for the app yet. Today that's exactly GPay and
 * PhonePe; every other allow-listed package resolves to null and is silently ignored.
 */
@Singleton
class ScraperRegistry @Inject constructor(
    gPayScreenScraper: GPayScreenScraper,
    phonePeScreenScraper: PhonePeScreenScraper,
) {
    private val scrapersByPackage: Map<String, ScreenScraper> =
        listOf(gPayScreenScraper, phonePeScreenScraper).associateBy { it.packageName }

    fun resolve(packageName: String): ScreenScraper? = scrapersByPackage[packageName]

    /** Packages that have a registered scraper — used by Settings to show the screen-reading health signal. */
    val supportedPackages: Set<String> get() = scrapersByPackage.keys
}
