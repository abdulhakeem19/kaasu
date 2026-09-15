package com.kaasu.app.domain.usecase.transaction

import com.kaasu.app.domain.repository.TransactionRepository
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    /**
     * Deleting one leg of a transfer deletes the movement.
     *
     * Leaving the other half behind would strand it as an unexplained debit or credit that counts
     * against the wrong side of the ledger — the exact state transfer groups exist to prevent.
     */
    suspend operator fun invoke(id: Long) {
        val transaction = repository.getById(id)
        val groupId = transaction?.transferGroupId
        if (groupId != null) {
            repository.deleteByTransferGroup(groupId)
        } else {
            repository.deleteById(id)
        }
    }
}
