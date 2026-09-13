package com.kaasu.app.feature.statement

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.statement.StatementImportManager
import com.kaasu.app.statement.UnrecognizedStatementFormatException
import com.kaasu.app.statement.model.ImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ImportStatementUiState {
    data object Loading : ImportStatementUiState
    data class Preview(val result: ImportResult) : ImportStatementUiState
    data class Error(val message: String) : ImportStatementUiState
    data class Committed(val insertedCount: Int) : ImportStatementUiState
}

@HiltViewModel
class ImportStatementViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val statementImportManager: StatementImportManager,
) : ViewModel() {

    private val uri: Uri = Uri.parse(savedStateHandle.get<String>("uri").orEmpty())
    private val mimeType: String? = savedStateHandle.get<String>("mimeType")
        ?.takeIf { it.isNotBlank() && it != "null" }

    private val _uiState = MutableStateFlow<ImportStatementUiState>(ImportStatementUiState.Loading)
    val uiState: StateFlow<ImportStatementUiState> = _uiState.asStateFlow()

    init {
        loadPreview()
    }

    private fun loadPreview() {
        _uiState.value = ImportStatementUiState.Loading
        viewModelScope.launch {
            runCatching { statementImportManager.preview(uri, mimeType) }
                .onSuccess { _uiState.value = ImportStatementUiState.Preview(it) }
                .onFailure { e -> _uiState.value = ImportStatementUiState.Error(e.toUserMessage()) }
        }
    }

    fun commit() {
        val current = _uiState.value
        if (current !is ImportStatementUiState.Preview) return
        viewModelScope.launch {
            runCatching { statementImportManager.commit(current.result) }
                .onSuccess { count -> _uiState.value = ImportStatementUiState.Committed(count) }
                .onFailure { e -> _uiState.value = ImportStatementUiState.Error(e.toUserMessage()) }
        }
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is UnrecognizedStatementFormatException -> message ?: "Unrecognized statement format."
        else -> message ?: "Couldn't read the selected file."
    }
}
