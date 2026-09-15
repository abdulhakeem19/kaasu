package com.kaasu.app.backfill

import com.kaasu.app.core.database.dao.AccountDao
import com.kaasu.app.core.database.dao.TransactionDao
import com.kaasu.app.domain.model.TransferRole
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repairs transfers captured before transfer groups existed.
 *
 * Until migration 8→9 the only record that two rows were halves of one movement was an arrow
 * written into `merchantName` — "IDFC FIRST → Union Bank". That string is all the history there is,
 * so this splits it back on the arrow, matches each half against the account list by name, and
 * rebuilds the group the rows should have had.
 *
 * Deliberately Kotlin rather than migration SQL: it needs fuzzy name matching against a table it
 * would have to join on, it must be safe to re-run, and a migration that throws leaves the database
 * unopenable. A repair that quietly does nothing is recoverable; a migration that crashes is not.
 *
 * Rows it cannot pair keep their arrow and get no role, so they surface in the half-linked list for
 * the owner to finish by hand rather than being silently guessed at.
 */
@Singleton
class TransferBackfillManager @Inject constructor(
    private val dao: TransactionDao,
    private val accountDao: AccountDao,
) {

    data class Result(
        val scanned: Int,
        val grouped: Int,
        val unpairable: Int,
    )

    suspend fun run(): Result {
        val rows = dao.getAllForBackup()
            .filter { it.transferGroupId == null && it.merchantName?.contains(ARROW) == true }
        if (rows.isEmpty()) return Result(0, 0, 0)

        val accounts = accountDao.getAllForBackup()
        var grouped = 0
        var unpairable = 0
        val now = System.currentTimeMillis()

        // Both legs of one movement carry the identical label and amount, so that pair is the key
        // that puts them back together.
        val byMovement = rows.groupBy { "${it.merchantName}|${it.amountInPaise}" }

        for ((_, legs) in byMovement) {
            val label = legs.first().merchantName ?: continue
            val fromName = label.substringBefore(ARROW).trim()
            val toName = label.substringAfter(ARROW).trim()

            val fromId = accounts.firstOrNull { it.displayName.equals(fromName, ignoreCase = true) }?.id
            val toId = accounts.firstOrNull { it.displayName.equals(toName, ignoreCase = true) }?.id
            if (fromId == null || toId == null) {
                unpairable += legs.size
                continue
            }

            val groupId = UUID.randomUUID().toString()
            for (leg in legs) {
                // The leg's own account decides its direction. Anything sitting on neither account
                // is left alone — guessing a role would move money in the wrong direction.
                val role = when (leg.accountId) {
                    fromId -> TransferRole.OUT
                    toId -> TransferRole.IN
                    else -> null
                }
                if (role == null) {
                    unpairable++
                    continue
                }
                dao.update(
                    leg.copy(
                        transferGroupId = groupId,
                        transferRole = role.name,
                        counterpartAccountId = if (role == TransferRole.OUT) toId else fromId,
                        // The arrow is dropped from the name now that the ids carry the direction;
                        // the label is rebuilt from the accounts at display time.
                        merchantName = null,
                        categoryId = null,
                        updatedAt = now,
                    )
                )
                grouped++
            }
        }

        return Result(scanned = rows.size, grouped = grouped, unpairable = unpairable)
    }

    private companion object {
        const val ARROW = "→"
    }
}
