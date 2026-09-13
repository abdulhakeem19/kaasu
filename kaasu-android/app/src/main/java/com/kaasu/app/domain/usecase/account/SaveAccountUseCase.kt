package com.kaasu.app.domain.usecase.account

import com.kaasu.app.domain.model.Account
import com.kaasu.app.domain.repository.AccountRepository
import javax.inject.Inject

class SaveAccountUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    suspend operator fun invoke(account: Account): Long =
        if (account.id == 0L) repository.insert(account)
        else { repository.update(account); account.id }
}
