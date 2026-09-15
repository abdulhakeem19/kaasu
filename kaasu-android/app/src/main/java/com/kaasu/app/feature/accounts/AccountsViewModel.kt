package com.kaasu.app.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.domain.model.AccountBalance
import com.kaasu.app.domain.repository.AccountRepository
import com.kaasu.app.domain.usecase.account.DeleteAccountUseCase
import com.kaasu.app.domain.usecase.account.GetAccountBalancesUseCase
import com.kaasu.app.domain.usecase.account.SaveAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsUiState(
    val balances: List<AccountBalance> = emptyList(),
    val isLoading: Boolean = true
) {
    val accounts get() = balances.mapNotNull { it.account }

    /** What is actually held, excluding what is owed on cards. */
    val netWorthInPaise: Long
        get() = balances.filter { it.isKnown && !it.isCreditCard }.sumOf { it.balanceInPaise }

    val totalOwedInPaise: Long
        get() = balances.filter { it.isCreditCard }.sumOf { it.outstandingInPaise }

    /** True once at least one account has an opening balance, so a total means something. */
    val hasAnyKnownBalance: Boolean get() = balances.any { it.isKnown }
}

@HiltViewModel
class AccountsViewModel @Inject constructor(
    getAccountBalances: GetAccountBalancesUseCase,
    private val deleteAccount: DeleteAccountUseCase,
    private val saveAccount: SaveAccountUseCase,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    val uiState = getAccountBalances()
        .map { AccountsUiState(balances = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AccountsUiState()
        )

    fun delete(id: Long) {
        viewModelScope.launch { deleteAccount(id) }
    }

    /**
     * Re-anchors an account to the balance its bank last stated.
     *
     * This one tap *is* the whole reconciliation mechanism — no drift detection, no matching engine.
     * A disagreement means Kaasu missed a message; rather than trying to work out which, it accepts
     * the bank's figure as the new starting point and counts forward from there.
     */
    fun useStatedBalance(accountId: Long) {
        viewModelScope.launch {
            val account = accountRepository.getById(accountId) ?: return@launch
            val stated = account.lastStatedBalanceInPaise ?: return@launch
            saveAccount(
                account.copy(
                    openingBalanceInPaise = stated,
                    // Anchored at the moment the bank said it, not now: transactions between then
                    // and now still have to be applied on top.
                    openingBalanceAt = account.lastStatedBalanceAt ?: System.currentTimeMillis(),
                )
            )
        }
    }
}
