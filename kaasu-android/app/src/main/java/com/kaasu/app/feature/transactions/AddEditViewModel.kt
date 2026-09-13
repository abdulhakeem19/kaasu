package com.kaasu.app.feature.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.core.util.parseToPaise
import com.kaasu.app.core.util.toAmountDisplay
import com.kaasu.app.domain.model.Account
import com.kaasu.app.domain.model.Category
import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.domain.usecase.account.GetAccountsUseCase
import com.kaasu.app.domain.usecase.transaction.GetAllCategoriesUseCase
import com.kaasu.app.domain.usecase.transaction.GetTransactionByIdUseCase
import com.kaasu.app.domain.usecase.transaction.RenameMerchantUseCase
import com.kaasu.app.domain.usecase.transaction.SaveTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditUiState(
    val amountText: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val merchantName: String = "",
    val categoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val accountId: Long? = null,
    val accounts: List<Account> = emptyList(),
    val note: String = "",
    val transactionTime: Long = System.currentTimeMillis(),
    val isEditMode: Boolean = false,
    // Original parsed merchant name at load — the key for the rename alias.
    val originalMerchant: String = "",
    val renameEverywhere: Boolean = true,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val isLoading: Boolean = true
) {
    // Show the "rename everywhere" option only when editing and the merchant has actually changed.
    val showRenameOption: Boolean get() = isEditMode && originalMerchant.isNotBlank() &&
        merchantName.trim().isNotBlank() && !merchantName.trim().equals(originalMerchant, ignoreCase = true)
}

@HiltViewModel
class AddEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTransactionById: GetTransactionByIdUseCase,
    private val getCategories: GetAllCategoriesUseCase,
    private val getAccounts: GetAccountsUseCase,
    private val saveTransaction: SaveTransactionUseCase,
    private val renameMerchant: RenameMerchantUseCase,
) : ViewModel() {

    private val transactionId: Long? = savedStateHandle.get<Long>("transactionId")?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val categories = getCategories().first()
            val accounts = getAccounts().first()
            if (transactionId != null) {
                val tx = getTransactionById(transactionId)
                _uiState.update {
                    it.copy(
                        amountText = tx?.amountInPaise?.toAmountDisplay() ?: "",
                        type = tx?.type ?: TransactionType.EXPENSE,
                        merchantName = tx?.merchantName ?: "",
                        originalMerchant = tx?.merchantName ?: "",
                        categoryId = tx?.categoryId,
                        accountId = tx?.accountId,
                        note = tx?.note ?: "",
                        transactionTime = tx?.transactionTime ?: System.currentTimeMillis(),
                        isEditMode = true,
                        categories = categories,
                        accounts = accounts,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(categories = categories, accounts = accounts, isLoading = false) }
            }
        }
    }

    fun onAmountChange(text: String) { _uiState.update { it.copy(amountText = text, errorMessage = null) } }
    fun onTypeChange(type: TransactionType) { _uiState.update { it.copy(type = type) } }
    fun onMerchantChange(name: String) { _uiState.update { it.copy(merchantName = name) } }
    fun onCategoryChange(id: Long?) { _uiState.update { it.copy(categoryId = id) } }
    fun onAccountChange(id: Long?) { _uiState.update { it.copy(accountId = id) } }
    fun onDateChange(time: Long) { _uiState.update { it.copy(transactionTime = time) } }
    fun onNoteChange(note: String) { _uiState.update { it.copy(note = note) } }
    fun onRenameEverywhereChange(value: Boolean) { _uiState.update { it.copy(renameEverywhere = value) } }

    fun save() {
        val state = _uiState.value
        val amountInPaise = state.amountText.parseToPaise()
        if (amountInPaise == null) {
            _uiState.update { it.copy(errorMessage = "Enter a valid amount") }
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val transaction = Transaction(
                id = if (state.isEditMode) transactionId!! else 0L,
                amountInPaise = amountInPaise,
                type = state.type,
                merchantName = state.merchantName.trim().ifBlank { null },
                categoryId = state.categoryId,
                sourceAppPackage = null,
                sourceAppName = null,
                paymentMode = null,
                rawTextHash = null,
                confidenceScore = 100,
                transactionTime = state.transactionTime,
                createdAt = now,
                updatedAt = now,
                isManual = true,
                isTransfer = state.type == TransactionType.TRANSFER,
                isRefund = state.type == TransactionType.REFUND || state.type == TransactionType.CASHBACK,
                isIgnored = false,
                note = state.note.trim().ifBlank { null },
                accountId = state.accountId
            )
            saveTransaction(transaction)

            // Remember the rename for future captures + apply to existing transactions of this merchant.
            val newMerchant = state.merchantName.trim()
            if (state.isEditMode && state.renameEverywhere && state.originalMerchant.isNotBlank() &&
                newMerchant.isNotBlank() && !newMerchant.equals(state.originalMerchant, ignoreCase = true)
            ) {
                renameMerchant(state.originalMerchant, newMerchant)
            }

            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
