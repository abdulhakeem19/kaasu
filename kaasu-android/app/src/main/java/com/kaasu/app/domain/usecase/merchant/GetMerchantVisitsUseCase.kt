package com.kaasu.app.domain.usecase.merchant

import com.kaasu.app.core.util.MerchantSimilarity
import com.kaasu.app.domain.model.MerchantVisits
import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.money.SpendRules
import com.kaasu.app.core.database.dao.MerchantAliasDao
import com.kaasu.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Counts how often a merchant has been paid.
 *
 * Grouped on read rather than kept as a `visitCount` column: a denormalised counter has to be
 * adjusted by every path that inserts, edits, deletes, splits, imports or de-duplicates a
 * transaction, and goes stale the first time one forgets — the same reasoning that keeps account
 * balances derived. If it is ever slow, the fix is a `GROUP BY` query, not a stored number.
 *
 * Matching reuses [MerchantSimilarity], so "Swiggy" and "Swiggy Instamart" count as one merchant,
 * exactly as the dedup paths already treat them. Renames are followed through the alias table, so
 * tidying a name up does not reset its history to one visit.
 */
class GetMerchantVisitsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val merchantAliasDao: MerchantAliasDao,
) {
    suspend operator fun invoke(
        merchantName: String?,
        windowStartMillis: Long? = null,
        windowEndMillis: Long? = null,
    ): MerchantVisits? {
        val name = merchantName?.trim()?.takeIf { it.isNotEmpty() } ?: return null

        // A merchant the owner renamed should still count its earlier spelling.
        val aliases = runCatching { merchantAliasDao.getAll() }.getOrDefault(emptyList())
        val names = buildSet {
            add(name)
            aliases.filter { it.displayName.equals(name, ignoreCase = true) }
                .forEach { add(it.originalLower) }
        }

        val matches = transactionRepository.getAll().first().filter { tx ->
            // Only real spending counts as a visit. A transfer is not a purchase, and an ignored or
            // duplicate row would inflate the habit it is meant to describe.
            SpendRules.isSpend(tx) && names.any { MerchantSimilarity.areSimilar(tx.merchantName, it) }
        }
        if (matches.isEmpty()) return null

        val inWindow = matches.filter { it.inWindow(windowStartMillis, windowEndMillis) }

        return MerchantVisits(
            merchantName = name,
            totalVisits = matches.size,
            totalSpentInPaise = matches.sumOf { it.amountInPaise },
            visitsInWindow = inWindow.size,
            spentInWindowInPaise = inWindow.sumOf { it.amountInPaise },
            firstVisitAt = matches.minOfOrNull { it.transactionTime },
            lastVisitAt = matches.maxOfOrNull { it.transactionTime },
        )
    }

    private fun Transaction.inWindow(start: Long?, end: Long?): Boolean =
        (start == null || transactionTime >= start) && (end == null || transactionTime <= end)
}
