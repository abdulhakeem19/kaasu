package com.kaasu.app.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.domain.model.Account
import com.kaasu.app.domain.usecase.account.DeleteAccountUseCase
import com.kaasu.app.domain.usecase.account.GetAccountsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    getAccounts: GetAccountsUseCase,
    private val deleteAccount: DeleteAccountUseCase,
) : ViewModel() {

    val uiState = getAccounts()
        .map { AccountsUiState(accounts = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AccountsUiState()
        )

    fun delete(id: Long) {
        viewModelScope.launch { deleteAccount(id) }
    }
}
