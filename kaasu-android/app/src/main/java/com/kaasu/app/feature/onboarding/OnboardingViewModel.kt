package com.kaasu.app.feature.onboarding

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.core.backup.BackupManager
import com.kaasu.app.core.datastore.SettingsDataStore
import com.kaasu.app.core.util.parseToPaise
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val page: Int = 0,
    val budgetInput: String = "",
    val isDone: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val backupManager: BackupManager,
) : ViewModel() {

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message = _message.asSharedFlow()

    // Restore a backup during onboarding; on success, skip straight into the app.
    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching { backupManager.importFromUri(uri) }
                .onSuccess {
                    settingsDataStore.setOnboardingComplete(true)
                    _uiState.update { s -> s.copy(isDone = true) }
                }
                .onFailure { _message.emit("Restore failed: ${it.message}") }
        }
    }

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    fun nextPage() { _uiState.update { it.copy(page = it.page + 1) } }

    fun onBudgetInputChange(input: String) { _uiState.update { it.copy(budgetInput = input) } }

    // Called when the user finishes the last onboarding page.
    // Saves budget (if entered) and marks onboarding complete, which triggers AppViewModel
    // to route to Dashboard via the splash gate in AppNavHost.
    fun complete() {
        viewModelScope.launch {
            val paise = _uiState.value.budgetInput.parseToPaise()
            if (paise != null && paise > 0) settingsDataStore.setMonthlyBudget(paise)
            settingsDataStore.setOnboardingComplete(true)
            _uiState.update { it.copy(isDone = true) }
        }
    }
}
