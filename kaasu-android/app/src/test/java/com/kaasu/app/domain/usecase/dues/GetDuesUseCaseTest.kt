package com.kaasu.app.domain.usecase.dues

import com.kaasu.app.domain.model.Account
import com.kaasu.app.domain.model.AccountBalance
import com.kaasu.app.domain.model.AccountType
import com.kaasu.app.domain.model.Due
import com.kaasu.app.domain.usecase.subscription.Subscription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GetDuesUseCaseTest {

    private val useCase = GetDuesUseCase()
    private val zone = ZoneId.of("Asia/Kolkata")

    /** 10 Sep 2025, 09:00 IST — mid-month, so "next due day" can fall either side. */
    private val now = LocalDate.of(2025, 9, 10).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()

    private fun card(
        id: Long = 9L,
        name: String = "SBI Card",
        dueDay: Int? = 5,
        outstanding: Long = 239_900L,
    ) = AccountBalance(
        account = Account(
            id = id, displayName = name, lastFourDigits = "1234",
            accountType = AccountType.CREDIT_CARD, colorArgb = null, isActive = true,
            createdAt = 0L, openingBalanceInPaise = 0L, openingBalanceAt = 0L, dueDay = dueDay,
        ),
        // Negative balance is what "outstanding" is made of.
        balanceInPaise = -outstanding,
        transactionCount = 1,
    )

    private fun subscription(name: String, amount: Long, nextAt: Long) = Subscription(
        merchantName = name, typicalAmountInPaise = amount, firstChargedAt = 0L,
        lastChargedAt = 0L, estimatedNextChargeAt = nextAt, cadenceDays = 30,
        chargeCount = 3, totalSpentInPaise = amount * 3, isManuallyFlagged = false,
        daysSinceLastCharge = 5,
    )

    // ── Card bills ────────────────────────────────────────────────────────────

    @Test fun `a card due day already past this month rolls to next month`() {
        // Due on the 5th, today is the 10th — the next one is 5 Oct, not a date in the past.
        val dues = useCase(listOf(card(dueDay = 5)), emptyList(), now, zone = zone)
        val due = dues.single()
        val date = java.time.Instant.ofEpochMilli(due.dueAt).atZone(zone).toLocalDate()
        assertEquals(LocalDate.of(2025, 10, 5), date)
    }

    @Test fun `a card due day still ahead stays in this month`() {
        val dues = useCase(listOf(card(dueDay = 18)), emptyList(), now, zone = zone)
        val date = java.time.Instant.ofEpochMilli(dues.single().dueAt).atZone(zone).toLocalDate()
        assertEquals(LocalDate.of(2025, 9, 18), date)
    }

    @Test fun `a card with nothing outstanding does not nag`() {
        assertTrue(useCase(listOf(card(outstanding = 0L)), emptyList(), now, zone = zone).isEmpty())
    }

    @Test fun `a card with no due day set produces nothing`() {
        assertTrue(useCase(listOf(card(dueDay = null)), emptyList(), now, zone = zone).isEmpty())
    }

    @Test fun `a due day past the end of a short month is clamped`() {
        // Due on the 31st, checked in February: the 28th, not a crash and not a skipped month.
        val feb = LocalDate.of(2026, 2, 1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val dues = useCase(listOf(card(dueDay = 31)), emptyList(), feb, zone = zone)
        val date = java.time.Instant.ofEpochMilli(dues.single().dueAt).atZone(zone).toLocalDate()
        assertEquals(LocalDate.of(2026, 2, 28), date)
    }

    @Test fun `the card due carries the outstanding amount and its account`() {
        val due = useCase(listOf(card(outstanding = 239_900L)), emptyList(), now, zone = zone).single()
        assertEquals(239_900L, due.amountInPaise)
        assertEquals(Due.Kind.CARD_BILL, due.kind)
        assertEquals(9L, due.accountId)
    }

    // ── Subscriptions ─────────────────────────────────────────────────────────

    @Test fun `an upcoming renewal is listed`() {
        val dues = useCase(
            emptyList(),
            listOf(subscription("Claude", 239_900L, now + 4 * Due.DAY_MS)),
            now, zone = zone,
        )
        assertEquals(Due.Kind.SUBSCRIPTION, dues.single().kind)
        assertEquals(4, dues.single().daysUntil(now))
    }

    @Test fun `a renewal beyond the horizon is not shown yet`() {
        val dues = useCase(
            emptyList(),
            listOf(subscription("Claude", 239_900L, now + 90 * Due.DAY_MS)),
            now, horizonDays = 30, zone = zone,
        )
        assertTrue(dues.isEmpty())
    }

    @Test fun `something already overdue is kept regardless of the horizon`() {
        // These matter most; dropping them for being in the past hides what needs attention.
        val dues = useCase(
            emptyList(),
            listOf(subscription("Claude", 239_900L, now - 3 * Due.DAY_MS)),
            now, zone = zone,
        )
        assertTrue(dues.single().isOverdue(now))
    }

    // ── Ordering and wording ──────────────────────────────────────────────────

    @Test fun `dues are ordered soonest first`() {
        val dues = useCase(
            listOf(card(dueDay = 18)),
            listOf(subscription("Claude", 100L, now + 2 * Due.DAY_MS)),
            now, zone = zone,
        )
        assertEquals(listOf("Claude", "SBI Card"), dues.map { it.title })
    }

    @Test fun `the wording reads the way a person would say it`() {
        fun labelFor(offsetDays: Long) = Due(
            title = "x", amountInPaise = 1L, dueAt = now + offsetDays * Due.DAY_MS,
            kind = Due.Kind.SUBSCRIPTION,
        ).whenLabel(now)

        assertEquals("due today", labelFor(0))
        assertEquals("due tomorrow", labelFor(1))
        assertEquals("due in 4 days", labelFor(4))
        assertEquals("overdue", labelFor(-2))
    }
}
