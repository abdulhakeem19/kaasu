package com.kaasu.app.domain.usecase.transaction

import com.kaasu.app.domain.repository.TransactionRepository
import javax.inject.Inject

class GetTransactionRawTextUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(id: Long): String? = repository.getRawText(id)
}
