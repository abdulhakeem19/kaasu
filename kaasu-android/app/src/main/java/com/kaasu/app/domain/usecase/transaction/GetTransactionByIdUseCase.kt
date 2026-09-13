package com.kaasu.app.domain.usecase.transaction

import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.repository.TransactionRepository
import javax.inject.Inject

class GetTransactionByIdUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(id: Long): Transaction? = repository.getById(id)
}
