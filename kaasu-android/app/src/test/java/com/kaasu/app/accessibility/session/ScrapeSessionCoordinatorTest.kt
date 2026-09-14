package com.kaasu.app.accessibility.session

import com.kaasu.app.accessibility.model.ScrapedTransactionCandidate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Every case here is a bug that previously escaped the unit tests and was only found by installing
 * the app, scrolling Google Pay by hand, and reading the database afterwards. The coordinator
 * exists so that loop is a few milliseconds instead of a few minutes.
 */
class ScrapeSessionCoordinatorTest {

    /** Stands in for the capture pipeline: stores everything, and counts what it was asked to store. */
    private class RecordingSink : ScrapedTransactionSink {
        val stored = mutableListOf<String>()
        var refuseAll = false
        override suspend fun insert(
            raw: com.kaasu.app.notification.model.RawNotification,
            merchant: String?,
            coarseBudget: MutableMap<String, Int>,
        ): Boolean {
            if (refuseAll) return false
            stored += "${merchant.orEmpty()}|${raw.text.orEmpty()}"
            return true
        }
    }

    private fun row(merchant: String, amount: String, date: String = "13 September") =
        ScrapedTransactionCandidate(
            amountText = amount,
            merchantText = merchant,
            dateText = date,
            directionHint = "debited",
            rawNodeText = "$merchant $amount debited $date",
            sourcePackage = "com.google.android.apps.nbu.paisa.user",
        )

    @Test
    fun `rows seen again after a scroll are not stored twice`() = runTest {
        val sink = RecordingSink()
        val coordinator = ScrapeSessionCoordinator(sink) { 1_000L }

        val screenOne = listOf(row("Tea Stall", "₹20"), row("Bakery", "₹40"))
        // Scrolling re-reads the rows that were already on screen, plus one new one below them.
        val screenTwo = listOf(row("Bakery", "₹40"), row("Fish Shop", "₹300"))

        assertEquals(2, coordinator.onScreen(screenOne))
        assertEquals(1, coordinator.onScreen(screenTwo))
        assertEquals(3, sink.stored.size)
    }

    @Test
    fun `the same screen read repeatedly stores nothing more`() = runTest {
        val sink = RecordingSink()
        val coordinator = ScrapeSessionCoordinator(sink) { 1_000L }
        val screen = listOf(row("Tea Stall", "₹20"))

        // One screen fires several content-changed events in a row.
        assertEquals(1, coordinator.onScreen(screen))
        assertEquals(0, coordinator.onScreen(screen))
        assertEquals(0, coordinator.onScreen(screen))
        assertEquals(1, sink.stored.size)
    }

    @Test
    fun `a row with no merchant is skipped rather than stored unnamed`() = runTest {
        val sink = RecordingSink()
        val coordinator = ScrapeSessionCoordinator(sink) { 1_000L }

        val nameless = row("Tea Stall", "₹20").copy(merchantText = null)
        val blank = row("Bakery", "₹40").copy(merchantText = "   ")

        assertEquals(0, coordinator.onScreen(listOf(nameless, blank)))
        assertEquals(0, sink.stored.size)
    }

    @Test
    fun `the coarse budget is shared across screens in one session`() = runTest {
        val sink = RecordingSink()
        val coordinator = ScrapeSessionCoordinator(sink) { 1_000L }
        coordinator.onScreen(listOf(row("Tea Stall", "₹20")))

        // A second screen must reuse the same budget map, so the stored rows that already cover an
        // amount are counted once for the session rather than re-counted per screen.
        val budgets = mutableSetOf<MutableMap<String, Int>>()
        val capturing = object : ScrapedTransactionSink {
            override suspend fun insert(
                raw: com.kaasu.app.notification.model.RawNotification,
                merchant: String?,
                coarseBudget: MutableMap<String, Int>,
            ): Boolean { budgets += coarseBudget; return true }
        }
        val shared = ScrapeSessionCoordinator(capturing) { 1_000L }
        shared.onScreen(listOf(row("A", "₹20")))
        shared.onScreen(listOf(row("B", "₹40")))
        assertEquals(1, budgets.size)
    }

    @Test
    fun `a later sitting starts a fresh session and may store the same rows again`() = runTest {
        val sink = RecordingSink()
        var clock = 1_000L
        val coordinator = ScrapeSessionCoordinator(sink) { clock }
        val screen = listOf(row("Tea Stall", "₹20"))

        assertEquals(1, coordinator.onScreen(screen))
        assertEquals(0, coordinator.onScreen(screen))

        // Hours later the owner opens GPay again. This is a new session: the coordinator must not
        // suppress the row forever, because whether it is a duplicate is the database's call.
        clock += 6 * 60 * 60 * 1000L
        assertEquals(1, coordinator.onScreen(screen))
    }

    @Test
    fun `rows the pipeline refuses are still marked handled`() = runTest {
        val sink = RecordingSink().apply { refuseAll = true }
        val coordinator = ScrapeSessionCoordinator(sink) { 1_000L }
        val screen = listOf(row("Tea Stall", "₹20"))

        assertEquals(0, coordinator.onScreen(screen))
        // Re-reading the same screen must not ask again — the answer will not have changed, and
        // asking repeatedly is what produced duplicate work on every content-changed event.
        assertEquals(0, coordinator.onScreen(screen))
        assertEquals(0, sink.stored.size)
    }
}
