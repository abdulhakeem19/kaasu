package com.kaasu.app.feature.subscriptions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.usecase.subscription.DetectSubscriptionsUseCase
import com.kaasu.app.domain.usecase.subscription.Subscription
import com.kaasu.app.domain.usecase.transaction.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SubscriptionDetailUiState(
    val subscription: Subscription? = null,
    val charges: List<Transaction> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SubscriptionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getTransactions: GetTransactionsUseCase,
    detectSubscriptions: DetectSubscriptionsUseCase,
) : ViewModel() {

    private val merchant: String = savedStateHandle.get<String>("merchant").orEmpty()

    val uiState = getTransactions()
        .map { transactions ->
            val sub = detectSubscriptions(transactions)
                .firstOrNull { it.merchantName.equals(merchant, ignoreCase = true) }
            val charges = transactions
                .filter { it.merchantName?.trim().equals(merchant, ignoreCase = true) }
                .sortedByDescending { it.transactionTime }
            SubscriptionDetailUiState(subscription = sub, charges = charges, isLoading = false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubscriptionDetailUiState())
}
