package com.kaasu.app.domain.usecase.transaction

import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.repository.TransactionRepository
import javax.inject.Inject

class SaveTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): Long {
        return if (transaction.id == 0L) {
            repository.insert(transaction)
        } else {
            repository.update(transaction)
            transaction.id
        }
    }
}
