package com.kaasu.app.feature.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.domain.usecase.privacy.DeleteAllDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrivacyDataUiState(
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false
)

@HiltViewModel
class PrivacyDataViewModel @Inject constructor(
    private val deleteAllData: DeleteAllDataUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivacyDataUiState())
    val uiState = _uiState.asStateFlow()

    // Observed by PrivacyDataScreen to show a success snackbar and pop back
    private val _deleteCompleteEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val deleteCompleteEvent: SharedFlow<Unit> = _deleteCompleteEvent.asSharedFlow()

    fun requestDelete() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = true)
    }

    fun dismissDelete() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = false)
    }

    fun confirmDelete() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = false, isDeleting = true)
        viewModelScope.launch {
            deleteAllData()
            _uiState.value = _uiState.value.copy(isDeleting = false)
            _deleteCompleteEvent.emit(Unit)
        }
    }
}
