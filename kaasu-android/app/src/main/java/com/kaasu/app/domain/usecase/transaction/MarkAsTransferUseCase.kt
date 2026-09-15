package com.kaasu.app.domain.usecase.transaction

import com.kaasu.app.core.database.dao.RuleDao
import com.kaasu.app.core.database.entity.RuleEntity
import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.domain.model.TransferRole
import com.kaasu.app.domain.repository.TransactionRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Reclassifies a payment the pipeline read as spending into a movement between the owner's accounts.
 *
 * This is the manual fallback for everything detection misses — an unusual bank phrasing, a card
 * whose issuer says nothing, a transfer whose two legs arrived too far apart to pair. Auto-detection
 * is deliberately conservative because a false positive erases real spending, so this is how the
 * remaining cases get corrected.
 *
 * When [rememberMerchant] is set, it also writes a rule, so next month's identical payment is caught
 * without being asked again. That is the point of the rule engine understanding type overrides:
 * correcting one card bill teaches every future one.
 */
class MarkAsTransferUseCase @Inject constructor(
    private val repository: TransactionRepository,
    private val ruleDao: RuleDao,
) {
    suspend operator fun invoke(
        transactionId: Long,
        counterpartAccountId: Long?,
        rememberMerchant: Boolean = false,
    ) {
        val transaction = repository.getById(transactionId) ?: return
        val now = System.currentTimeMillis()

        // Direction follows what the row already was: money that left stays money that left.
        val role = when (transaction.type) {
            TransactionType.INCOME, TransactionType.REFUND, TransactionType.CASHBACK -> TransferRole.IN
            else -> TransferRole.OUT
        }

        repository.update(
            transaction.copy(
                type = TransactionType.TRANSFER,
                isTransfer = true,
                transferGroupId = transaction.transferGroupId ?: UUID.randomUUID().toString(),
                transferRole = role,
                counterpartAccountId = counterpartAccountId,
                // A transfer is not spending, so whatever category it was filed under no longer
                // applies — leaving it would keep inflating that category's budget.
                categoryId = null,
                updatedAt = now,
            )
        )

        val merchant = transaction.merchantName?.trim()
        if (rememberMerchant && !merchant.isNullOrBlank()) {
            ruleDao.insert(
                RuleEntity(
                    name = merchant,
                    matchText = merchant,
                    matchType = "EQUALS",
                    categoryId = null,
                    transactionType = TransactionType.TRANSFER.name,
                    sourceAppPackage = null,
                    // Above the seeded rules (20) so a correction the owner made by hand wins.
                    priority = 100,
                    isSystem = false,
                    isActive = true,
                    createdAt = now,
                    updatedAt = now,
                )
            )
        }
    }
}
