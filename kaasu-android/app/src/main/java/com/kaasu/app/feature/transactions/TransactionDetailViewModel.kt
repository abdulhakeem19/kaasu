package com.kaasu.app.feature.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.domain.model.Account
import com.kaasu.app.domain.model.Category
import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.repository.AccountRepository
import com.kaasu.app.domain.repository.SplitSlice
import com.kaasu.app.domain.repository.TransactionRepository
import com.kaasu.app.domain.usecase.transaction.DeleteTransactionUseCase
import com.kaasu.app.domain.usecase.transaction.GetAllCategoriesUseCase
import com.kaasu.app.domain.usecase.transaction.GetTransactionRawTextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import com.kaasu.app.domain.usecase.transaction.MarkAsTransferUseCase
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val transaction: Transaction? = null,
    val category: Category? = null,
    val account: Account? = null,
    // Every account, for reclassifying this payment as a move between two of them.
    val accounts: List<Account> = emptyList(),
    val counterpartAccount: Account? = null,
    val capturedText: String? = null,
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val isSplit: Boolean = false,
    val isMarkedDuplicate: Boolean = false
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCategories: GetAllCategoriesUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val getRawText: GetTransactionRawTextUseCase,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val markAsTransfer: MarkAsTransferUseCase,
) : ViewModel() {

    private val transactionId: Long = checkNotNull(savedStateHandle["transactionId"])

    // One-off action outcomes (delete / split) that drive navigation away from this screen.
    private data class Actions(
        val isDeleted: Boolean = false,
        val isSplit: Boolean = false,
        val isMarkedDuplicate: Boolean = false
    )
    private val actions = MutableStateFlow(Actions())

    // Observe the transaction live so edits (amount, category, mode, …) reflect on return
    // instead of showing a one-shot snapshot loaded once at open.
    val uiState = combine(
        transactionRepository.observeById(transactionId),
        getCategories(),
        actions
    ) { tx, categories, actionState ->
        val category = tx?.categoryId?.let { id -> categories.find { it.id == id } }
        val account = tx?.accountId?.let { accountRepository.getById(it) }
        val counterpart = tx?.counterpartAccountId?.let { accountRepository.getById(it) }
        val allAccounts = accountRepository.getAll().first()
        val rawText = getRawText(transactionId)
        DetailUiState(
            transaction = tx,
            category = category,
            account = account,
            accounts = allAccounts,
            counterpartAccount = counterpart,
            capturedText = rawText,
            categories = categories,
            isLoading = false,
            isDeleted = actionState.isDeleted,
            isSplit = actionState.isSplit,
            isMarkedDuplicate = actionState.isMarkedDuplicate
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState()
    )

    fun toggleRecurring() {
        val current = uiState.value.transaction ?: return
        viewModelScope.launch {
            // The observed flow re-emits the updated row, so no optimistic local update needed.
            transactionRepository.setRecurring(current.id, !current.isRecurring)
        }
    }

    fun splitTransaction(slices: List<SplitSlice>) {
        if (slices.size < 2) return
        viewModelScope.launch {
            transactionRepository.splitTransaction(transactionId, slices)
            actions.value = actions.value.copy(isSplit = true)
        }
    }

    fun markDuplicate() {
        val current = uiState.value.transaction ?: return
        viewModelScope.launch {
            transactionRepository.setDuplicate(current.id, true)
            actions.value = actions.value.copy(isMarkedDuplicate = true)
        }
    }

    /**
     * Reclassifies this payment as money moved between the owner's own accounts.
     *
     * The manual counterpart to automatic detection, which is deliberately conservative — it would
     * rather miss a transfer than erase a real expense. [rememberMerchant] writes a rule so the
     * same payment is caught by itself next month.
     */
    fun markAsTransfer(counterpartAccountId: Long?, rememberMerchant: Boolean) {
        viewModelScope.launch {
            markAsTransfer(transactionId, counterpartAccountId, rememberMerchant)
        }
    }

    fun deleteTransaction() {
        viewModelScope.launch {
            deleteTransaction(transactionId)
            actions.value = actions.value.copy(isDeleted = true)
        }
    }
}
