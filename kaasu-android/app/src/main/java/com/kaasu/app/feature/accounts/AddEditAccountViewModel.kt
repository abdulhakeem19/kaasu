package com.kaasu.app.feature.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.core.util.toColorHex
import com.kaasu.app.domain.model.Account
import com.kaasu.app.core.util.parseToPaise
import com.kaasu.app.core.util.toAmountDisplay
import com.kaasu.app.domain.model.AccountType
import com.kaasu.app.domain.repository.AccountRepository
import com.kaasu.app.domain.usecase.account.SaveAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditAccountUiState(
    val displayName: String = "",
    val lastFourDigits: String = "",
    val accountType: AccountType = AccountType.SAVINGS,
    val colorHex: String = "#1E88E5",
    // Balances count forward from this. Blank means "don't claim to know", which is honest;
    // defaulting to 0 would present a guess as a fact.
    val openingBalanceText: String = "",
    // Credit cards only — asked once here because these never change.
    val creditLimitText: String = "",
    val statementDayText: String = "",
    val dueDayText: String = "",
    val isEditMode: Boolean = false,
    val isSaved: Boolean = false,
    val nameError: String? = null,
    val lastFourError: String? = null,
    val openingBalanceError: String? = null,
    val dayError: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class AddEditAccountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val saveAccount: SaveAccountUseCase,
) : ViewModel() {

    private val accountId: Long? = savedStateHandle.get<Long>("accountId")?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(AddEditAccountUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (accountId != null) {
                val account = accountRepository.getById(accountId)
                if (account != null) {
                    _uiState.update {
                        it.copy(
                            displayName = account.displayName,
                            lastFourDigits = account.lastFourDigits ?: "",
                            accountType = account.accountType,
                            colorHex = account.colorArgb?.toColorHex() ?: "#1E88E5",
                            openingBalanceText = account.openingBalanceInPaise?.toAmountDisplay() ?: "",
                            creditLimitText = account.creditLimitInPaise?.toAmountDisplay() ?: "",
                            statementDayText = account.statementDay?.toString() ?: "",
                            dueDayText = account.dueDay?.toString() ?: "",
                            isEditMode = true,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onDisplayNameChange(v: String) { _uiState.update { it.copy(displayName = v, nameError = null) } }
    fun onLastFourChange(v: String) {
        if (v.length <= 4 && v.all { it.isDigit() }) {
            _uiState.update { it.copy(lastFourDigits = v, lastFourError = null) }
        }
    }
    fun onAccountTypeChange(type: AccountType) { _uiState.update { it.copy(accountType = type) } }
    fun onColorChange(hex: String) { _uiState.update { it.copy(colorHex = hex) } }
    fun onOpeningBalanceChange(v: String) {
        _uiState.update { it.copy(openingBalanceText = v, openingBalanceError = null) }
    }
    fun onCreditLimitChange(v: String) { _uiState.update { it.copy(creditLimitText = v) } }
    fun onStatementDayChange(v: String) {
        if (v.length <= 2 && v.all { it.isDigit() }) {
            _uiState.update { it.copy(statementDayText = v, dayError = null) }
        }
    }
    fun onDueDayChange(v: String) {
        if (v.length <= 2 && v.all { it.isDigit() }) {
            _uiState.update { it.copy(dueDayText = v, dayError = null) }
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.displayName.isBlank()) {
            _uiState.update { it.copy(nameError = "Name is required") }
            return
        }
        val lastFour = state.lastFourDigits.takeIf { it.isNotBlank() }
        if (lastFour != null && lastFour.length != 4) {
            _uiState.update { it.copy(lastFourError = "Enter all 4 digits") }
            return
        }
        val openingText = state.openingBalanceText.trim()
        // Blank is allowed and means unset. A typed value that will not parse is an error rather
        // than a silent zero — "0" and "I mistyped" mean very different things for a balance.
        val openingBalance = if (openingText.isEmpty()) null else openingText.parseToPaise()
        if (openingText.isNotEmpty() && openingBalance == null) {
            _uiState.update { it.copy(openingBalanceError = "Enter a valid amount") }
            return
        }

        val statementDay = state.statementDayText.trim().toIntOrNull()
        val dueDay = state.dueDayText.trim().toIntOrNull()
        if (listOfNotNull(statementDay, dueDay).any { it !in 1..31 }) {
            _uiState.update { it.copy(dayError = "Use a day between 1 and 31") }
            return
        }

        viewModelScope.launch {
            val colorArgb = runCatching {
                android.graphics.Color.parseColor(state.colorHex)
            }.getOrNull()
            // Everything not shown on this screen is read back off the stored row, so editing a name
            // cannot quietly discard the opening balance, the bank's stated figure, or the date the
            // account was first seen.
            val existing = accountId?.let { accountRepository.getById(it) }
            val isCard = state.accountType == AccountType.CREDIT_CARD
            val account = Account(
                id = accountId ?: 0L,
                displayName = state.displayName.trim(),
                lastFourDigits = lastFour,
                accountType = state.accountType,
                colorArgb = colorArgb,
                isActive = existing?.isActive ?: true,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                openingBalanceInPaise = openingBalance,
                // Re-anchor the moment the figure changes: an opening balance is only meaningful
                // paired with the time it was true, and keeping a stale timestamp would silently
                // re-apply every transaction since.
                openingBalanceAt = when {
                    openingBalance == null -> null
                    existing?.openingBalanceInPaise == openingBalance -> existing.openingBalanceAt
                    else -> System.currentTimeMillis()
                },
                lastStatedBalanceInPaise = existing?.lastStatedBalanceInPaise,
                lastStatedBalanceAt = existing?.lastStatedBalanceAt,
                creditLimitInPaise = state.creditLimitText.trim().takeIf { isCard && it.isNotEmpty() }?.parseToPaise(),
                statementDay = statementDay?.takeIf { isCard },
                dueDay = dueDay?.takeIf { isCard },
            )
            saveAccount(account)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
