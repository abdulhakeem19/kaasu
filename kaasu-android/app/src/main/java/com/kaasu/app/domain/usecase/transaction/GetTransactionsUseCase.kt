package com.kaasu.app.domain.usecase.transaction

import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<List<Transaction>> = repository.getAll()
}
