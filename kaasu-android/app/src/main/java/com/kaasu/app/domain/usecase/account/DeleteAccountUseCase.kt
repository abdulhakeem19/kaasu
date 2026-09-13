package com.kaasu.app.domain.usecase.account

import com.kaasu.app.domain.repository.AccountRepository
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}
