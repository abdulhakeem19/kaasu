package com.kaasu.app.feature.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DuplicatesUiState(
    val items: List<Transaction> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DuplicatesViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    val uiState = transactionRepository.observeDuplicates()
        .map { DuplicatesUiState(items = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DuplicatesUiState())

    // Un-mark: clears the duplicate flag and un-hides the transaction so it counts again.
    fun restore(id: Long) {
        viewModelScope.launch { transactionRepository.setDuplicate(id, false) }
    }
}
