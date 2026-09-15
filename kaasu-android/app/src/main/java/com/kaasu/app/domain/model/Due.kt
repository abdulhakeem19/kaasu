package com.kaasu.app.domain.model

/**
 * Something money is expected to leave for, soon.
 *
 * Derived from what Kaasu already knows — a subscription's estimated next charge, a card's due day
 * against what is outstanding on it — rather than stored in a reminders table. There is no
 * scheduling here and nothing to keep in sync: the facts already exist, and a table of copies of
 * them would only drift from the transactions that produced them.
 */
data class Due(
    val title: String,
    val amountInPaise: Long,
    val dueAt: Long,
    val kind: Kind,
    /** For a card due, the account it belongs to. */
    val accountId: Long? = null,
) {
    enum class Kind { CARD_BILL, SUBSCRIPTION }

    fun daysUntil(now: Long = System.currentTimeMillis()): Int =
        Math.ceil((dueAt - now) / DAY_MS.toDouble()).toInt()

    fun isOverdue(now: Long = System.currentTimeMillis()): Boolean = dueAt < now

    /** "due in 4 days", "due tomorrow", "overdue" — the phrasing a person would actually use. */
    fun whenLabel(now: Long = System.currentTimeMillis()): String {
        val days = daysUntil(now)
        return when {
            days < 0 -> "overdue"
            days == 0 -> "due today"
            days == 1 -> "due tomorrow"
            else -> "due in $days days"
        }
    }

    companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
