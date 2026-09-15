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
import com.kaasu.app.domain.model.TransferRole
import com.kaasu.app.domain.repository.TransactionRepository
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
    // The receiving account, for a transfer. A transfer with only a "from" is a payment to nobody.
    val toAccountId: Long? = null,
    // Carried through an edit so the movement keeps its identity instead of being re-grouped.
    val transferGroupId: String? = null,
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
    val isTransfer: Boolean get() = type == TransactionType.TRANSFER

    /** Both ends are required, and they cannot be the same account. */
    val transferError: String? get() = when {
        !isTransfer -> null
        accountId == null -> "Choose the account the money leaves"
        toAccountId == null -> "Choose the account the money arrives in"
        accountId == toAccountId -> "Pick two different accounts"
        else -> null
    }

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
    private val transactionRepository: TransactionRepository,
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
                        // The editor always presents the row being edited as the leg money leaves,
                        // so opening the *arriving* leg swaps the two ends back into From/To order.
                        // The group still nets to zero either way — one OUT and one IN across the
                        // same two accounts — so which physical row holds which role is immaterial.
                        accountId = if (tx?.transferRole == TransferRole.IN) tx.counterpartAccountId else tx?.accountId,
                        toAccountId = if (tx?.transferRole == TransferRole.IN) tx.accountId else tx?.counterpartAccountId,
                        transferGroupId = tx?.transferGroupId,
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

    fun onToAccountChange(id: Long?) { _uiState.update { it.copy(toAccountId = id) } }
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

        state.transferError?.let { message ->
            _uiState.update { it.copy(errorMessage = message) }
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            // Both legs of a hand-entered transfer share one group, exactly as a captured pair does.
            // An edit keeps the group it already has — minting a new id would orphan the other leg.
            val groupId = when {
                !state.isTransfer -> null
                else -> state.transferGroupId ?: java.util.UUID.randomUUID().toString()
            }
            val existingLegs = groupId
                ?.takeIf { state.isEditMode }
                ?.let { transactionRepository.getByTransferGroup(it) }
                .orEmpty()
            val transaction = Transaction(
                id = if (state.isEditMode) transactionId!! else 0L,
                amountInPaise = amountInPaise,
                type = state.type,
                merchantName = state.merchantName.trim().ifBlank { null },
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
                accountId = state.accountId,
                transferGroupId = groupId,
                transferRole = if (state.isTransfer) TransferRole.OUT else null,
                counterpartAccountId = if (state.isTransfer) state.toAccountId else null,
                // A transfer is not spending, so it belongs to no spending category.
                categoryId = if (state.isTransfer) null else state.categoryId,
            )
            saveTransaction(transaction)

            // The arriving leg. Written as a second row rather than inferred, so the receiving
            // account's balance moves on its own row like every captured credit does.
            //
            // On an edit the sibling already exists and is updated in place: inserting again would
            // add a fresh IN leg on every save, and the group would drift further from the truth
            // each time it was opened.
            if (state.isTransfer && groupId != null) {
                val sibling = existingLegs.firstOrNull { it.id != transaction.id }
                saveTransaction(
                    transaction.copy(
                        id = sibling?.id ?: 0L,
                        createdAt = sibling?.createdAt ?: now,
                        transferRole = TransferRole.IN,
                        accountId = state.toAccountId,
                        counterpartAccountId = state.accountId,
                    )
                )
            }

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
