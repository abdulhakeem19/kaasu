package com.kaasu.app.domain.model

/**
 * How often a merchant has been paid, and how much.
 *
 * "8 visits · ₹3,240 this month" answers a question a single transaction cannot: whether this is a
 * habit or a one-off. That is the difference between a ₹300 coffee and ₹2,400 of coffee.
 */
data class MerchantVisits(
    val merchantName: String,
    /** Visits across all of history, which is what makes a habit visible. */
    val totalVisits: Int,
    val totalSpentInPaise: Long,
    /** Scoped to the window asked for — usually the current budget cycle. */
    val visitsInWindow: Int,
    val spentInWindowInPaise: Long,
    val firstVisitAt: Long?,
    val lastVisitAt: Long?,
) {
    val isRepeat: Boolean get() = totalVisits > 1

    val averageSpendInPaise: Long get() = if (totalVisits > 0) totalSpentInPaise / totalVisits else 0L
}
