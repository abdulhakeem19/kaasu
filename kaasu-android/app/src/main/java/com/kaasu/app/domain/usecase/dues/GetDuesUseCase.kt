package com.kaasu.app.domain.usecase.dues

import com.kaasu.app.domain.model.AccountBalance
import com.kaasu.app.domain.model.Due
import com.kaasu.app.domain.usecase.subscription.Subscription
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/**
 * Assembles what is coming up, out of facts the app already holds.
 *
 * Deliberately not a reminders table with its own scheduling. A card's due day and its outstanding
 * balance already exist, and so does a subscription's estimated next charge — copying them into
 * rows would create a second version of the truth that drifts the moment a transaction changes.
 */
class GetDuesUseCase @Inject constructor() {

    operator fun invoke(
        balances: List<AccountBalance>,
        subscriptions: List<Subscription>,
        now: Long = System.currentTimeMillis(),
        horizonDays: Int = 30,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Due> {
        // Not LocalDate.ofInstant — that is API 34 and minSdk is 26, so it would crash on most
        // devices. Lint caught it; the unit tests did not, since they run on the JVM.
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

        val cardDues = balances.mapNotNull { balance ->
            val account = balance.account ?: return@mapNotNull null
            val dueDay = account.dueDay ?: return@mapNotNull null
            // Nothing owed is nothing to chase. A card sitting at zero should not nag monthly.
            val outstanding = balance.outstandingInPaise.takeIf { it > 0 } ?: return@mapNotNull null

            Due(
                title = account.displayName,
                amountInPaise = outstanding,
                dueAt = nextOccurrence(dueDay, today, zone),
                kind = Due.Kind.CARD_BILL,
                accountId = account.id,
            )
        }

        val renewals = subscriptions.map { sub ->
            Due(
                title = sub.merchantName,
                amountInPaise = sub.typicalAmountInPaise,
                dueAt = sub.estimatedNextChargeAt,
                kind = Due.Kind.SUBSCRIPTION,
            )
        }

        val horizonEnd = now + horizonDays * Due.DAY_MS
        // Overdue items are kept regardless of the horizon — they are the ones that matter most,
        // and dropping them for being in the past would hide exactly what needs attention.
        return (cardDues + renewals)
            .filter { it.dueAt <= horizonEnd }
            .sortedBy { it.dueAt }
    }

    /**
     * The next time this day-of-month comes round, at the start of that day.
     *
     * Clamped to the month's length, so a card due on the 31st still resolves in February rather
     * than throwing or silently skipping a month.
     */
    private fun nextOccurrence(dueDay: Int, today: LocalDate, zone: ZoneId): Long {
        fun dateIn(month: YearMonth) = month.atDay(dueDay.coerceIn(1, month.lengthOfMonth()))

        val thisMonth = dateIn(YearMonth.from(today))
        val target = if (!thisMonth.isBefore(today)) thisMonth else dateIn(YearMonth.from(today).plusMonths(1))
        return target.atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
