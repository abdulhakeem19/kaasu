package com.kaasu.app.feature.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.core.util.toColorHex
import com.kaasu.app.domain.model.Account
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
    val isEditMode: Boolean = false,
    val isSaved: Boolean = false,
    val nameError: String? = null,
    val lastFourError: String? = null,
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
        viewModelScope.launch {
            val colorArgb = runCatching {
                android.graphics.Color.parseColor(state.colorHex)
            }.getOrNull()
            val account = Account(
                id = accountId ?: 0L,
                displayName = state.displayName.trim(),
                lastFourDigits = lastFour,
                accountType = state.accountType,
                colorArgb = colorArgb,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
            saveAccount(account)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
