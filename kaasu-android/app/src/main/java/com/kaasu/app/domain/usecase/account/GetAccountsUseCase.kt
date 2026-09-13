package com.kaasu.app.domain.usecase.account

import com.kaasu.app.domain.model.Account
import com.kaasu.app.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAccountsUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    operator fun invoke(): Flow<List<Account>> = repository.getAll()
}
