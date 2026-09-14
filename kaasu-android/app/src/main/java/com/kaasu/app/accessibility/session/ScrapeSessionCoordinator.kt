package com.kaasu.app.accessibility.session

import com.kaasu.app.accessibility.model.ScrapedTransactionCandidate
import com.kaasu.app.accessibility.model.toRawNotification
import com.kaasu.app.notification.model.RawNotification

/** What the coordinator needs from the capture pipeline, narrowed so tests can substitute it. */
fun interface ScrapedTransactionSink {
    /** Returns true if the candidate was stored. */
    suspend fun insert(
        raw: RawNotification,
        merchant: String?,
        coarseBudget: MutableMap<String, Int>,
    ): Boolean
}

/**
 * Owns the state that spans a single burst of screen reading, and the only part of this channel
 * that is genuinely stateful. Deliberately Android-free: every bug this class exists to prevent was
 * found by running the app on a phone, because nothing could exercise the behaviour otherwise.
 *
 * Two things have to persist across screens, not just within one:
 *
 *  - **Which rows have already been handled.** Scrolling re-reads rows that were on the previous
 *    screen. Judging each screen on its own filled a deliberate one-row gap with three rows.
 *  - **The coarse-duplicate budget**, so the stored rows that already cover an amount on a day are
 *    counted once for the whole session rather than re-counted per screen.
 *
 * A session ends after [SESSION_IDLE_MS] of no screens, which is what separates "the owner is
 * scrolling their history" from "they came back tomorrow".
 */
class ScrapeSessionCoordinator(
    private val sink: ScrapedTransactionSink,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val handledRows = mutableSetOf<String>()
    private val coarseBudget = mutableMapOf<String, Int>()
    private var lastScreenAt = 0L

    /** Processes one screen's worth of rows. Returns how many were stored. */
    suspend fun onScreen(candidates: List<ScrapedTransactionCandidate>): Int {
        val timestamp = now()
        if (timestamp - lastScreenAt > SESSION_IDLE_MS) reset()
        lastScreenAt = timestamp

        var inserted = 0
        for (candidate in candidates) {
            // Row identity is its own text: the same payment re-read after a scroll is the same
            // string, so this is what stops overlapping screens inserting it twice.
            if (!handledRows.add(candidate.rawNodeText)) continue

            // A scraped row always names who was paid. One that does not is a row this scraper
            // misread, and storing an unnamed amount is worse than missing it — the other three
            // channels would have caught a real payment anyway.
            val merchant = candidate.merchantText?.trim()
            if (merchant.isNullOrEmpty()) continue

            if (sink.insert(candidate.toRawNotification(), merchant, coarseBudget)) inserted++
        }
        return inserted
    }

    private fun reset() {
        handledRows.clear()
        coarseBudget.clear()
    }

    private companion object {
        /** Long enough to cover scrolling through history, short enough not to span two sittings. */
        const val SESSION_IDLE_MS = 2 * 60 * 1000L
    }
}
