package com.kaasu.app.accessibility.model

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScrapedTransactionCandidateTest {

    private val zone = ZoneId.systemDefault()

    @Test fun toRawNotification_wrapsFieldsIntoTheSharedRawNotificationModel() {
        val candidate = ScrapedTransactionCandidate(
            amountText = "₹245",
            merchantText = "Swiggy",
            dateText = "28 Aug",
            directionHint = "paid to",
            rawNodeText = "Paid to Swiggy ₹245 28 Aug",
            sourcePackage = "com.google.android.apps.nbu.paisa.user",
            scrapedAt = 1_000_000L
        )

        val raw = candidate.toRawNotification()

        assertEquals("com.google.android.apps.nbu.paisa.user", raw.packageName)
        assertNull(raw.appName)
        assertNull(raw.title)
        assertEquals("Paid to Swiggy ₹245 28 Aug", raw.text)
        assertNull(raw.subText)
    }

    @Test fun toRawNotification_resolvesPostedAtFromDateText_notScrapedAt() {
        // scrapedAt simulates "Kaasu happened to observe this row on a later date" (e.g. the owner
        // scrolled back through old history) — postedAt must reflect the row's own date, not the
        // moment it was scraped.
        val scrapedAt = Instant.parse("2026-09-05T10:00:00Z").toEpochMilli()
        val candidate = ScrapedTransactionCandidate(
            amountText = "₹245",
            merchantText = "Swiggy",
            dateText = "28 Aug 2026",
            directionHint = "paid to",
            rawNodeText = "Paid to Swiggy ₹245 28 Aug 2026",
            sourcePackage = "com.phonepe.app",
            scrapedAt = scrapedAt
        )

        val raw = candidate.toRawNotification()

        val resolvedDate = Instant.ofEpochMilli(raw.postedAt).atZone(zone).toLocalDate()
        assertEquals(java.time.LocalDate.of(2026, 8, 28), resolvedDate)
    }

    @Test fun toRawNotification_fallsBackToScrapedAtWhenDateTextUnparseable() {
        val candidate = ScrapedTransactionCandidate(
            amountText = "₹245",
            merchantText = null,
            dateText = null,
            directionHint = null,
            rawNodeText = "₹245 debited",
            sourcePackage = "com.phonepe.app",
            scrapedAt = 42_000L
        )

        val raw = candidate.toRawNotification()

        assertEquals(42_000L, raw.postedAt)
    }
}
