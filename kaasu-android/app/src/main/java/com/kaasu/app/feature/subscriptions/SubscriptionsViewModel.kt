package com.kaasu.app.feature.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.core.util.parseToPaise
import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.domain.repository.TransactionRepository
import com.kaasu.app.domain.usecase.subscription.DetectSubscriptionsUseCase
import com.kaasu.app.domain.usecase.subscription.Subscription
import com.kaasu.app.domain.usecase.transaction.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionsUiState(
    val monthlyTotalInPaise: Long = 0L,
    val activeCount: Int = 0,
    val unusedCount: Int = 0,
    val unusedTotalInPaise: Long = 0L,
    val renewsThisWeek: List<Subscription> = emptyList(),
    val unused: List<Subscription> = emptyList(),
    val otherActive: List<Subscription> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    getTransactions: GetTransactionsUseCase,
    detectSubscriptions: DetectSubscriptionsUseCase,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    // Adds a subscription the user enters by hand: stored as a recurring expense so it flows
    // through the same detection + monthly-total logic. The charge is dated to the most recent
    // occurrence of [renewDay] (1–28) so the detected "next due" lands on that day next month.
    // Returns false if the inputs are invalid.
    fun addManual(merchant: String, amountText: String, renewDay: Int, onResult: (Boolean) -> Unit) {
        val name = merchant.trim()
        val paise = amountText.parseToPaise()
        if (name.isBlank() || paise == null || paise <= 0 || renewDay !in 1..28) { onResult(false); return }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val today = java.time.LocalDate.now()
            // Most recent occurrence of the renewal day, on/before today.
            val lastCharge = (if (today.dayOfMonth >= renewDay) today.withDayOfMonth(renewDay)
                else today.minusMonths(1).withDayOfMonth(renewDay))
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            transactionRepository.insert(
                Transaction(
                    amountInPaise = paise,
                    type = TransactionType.EXPENSE,
                    merchantName = name,
                    categoryId = null,
                    sourceAppPackage = null,
                    sourceAppName = null,
                    paymentMode = null,
                    rawTextHash = null,
                    confidenceScore = 100,
                    transactionTime = lastCharge,
                    createdAt = now,
                    updatedAt = now,
                    isManual = true,
                    isRecurring = true,
                    note = "Manually added subscription"
                )
            )
            onResult(true)
        }
    }

    val uiState = getTransactions()
        .map { transactions ->
            val subs = detectSubscriptions(transactions)

            // Monthly run-rate normalises each subscription to a per-month figure.
            val monthlyTotal = subs.sumOf { it.typicalAmountInPaise * 30 / it.cadenceDays.coerceAtLeast(1) }
            val unused = subs.filter { it.isLikelyUnused }
            val active = subs.filterNot { it.isLikelyUnused }
            val renewsThisWeek = active.filter { it.daysUntilRenewal in 0..7 }
            val otherActive = active.filterNot { it.daysUntilRenewal in 0..7 }

            SubscriptionsUiState(
                monthlyTotalInPaise = monthlyTotal,
                activeCount = active.size,
                unusedCount = unused.size,
                unusedTotalInPaise = unused.sumOf { it.typicalAmountInPaise },
                renewsThisWeek = renewsThisWeek,
                unused = unused,
                otherActive = otherActive,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SubscriptionsUiState()
        )
}
