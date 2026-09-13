package com.kaasu.app.domain.usecase.transaction

import com.kaasu.app.domain.repository.TransactionRepository
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(id: Long) = repository.deleteById(id)
}
