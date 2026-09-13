package com.kaasu.app.feature.categories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.domain.model.Category
import com.kaasu.app.domain.model.CategoryType
import com.kaasu.app.domain.repository.CategoryRepository
import com.kaasu.app.domain.usecase.category.SaveCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditCategoryUiState(
    val name: String = "",
    val type: CategoryType = CategoryType.EXPENSE,
    val color: String = "#1E88E5",
    val isEditMode: Boolean = false,
    val isSaved: Boolean = false,
    val nameError: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class AddEditCategoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val categoryRepository: CategoryRepository,
    private val saveCategory: SaveCategoryUseCase,
) : ViewModel() {

    // -1L is the default nav arg value used in AppNavigation.kt to signal "add" mode vs a real category ID for "edit" mode
    private val categoryId: Long? = savedStateHandle.get<Long>("categoryId")?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(AddEditCategoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (categoryId != null) {
                val cat = categoryRepository.getById(categoryId)
                if (cat != null) {
                    _uiState.update {
                        it.copy(
                            name = cat.name,
                            type = cat.type,
                            color = cat.color ?: "#1E88E5",
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

    fun onNameChange(name: String) { _uiState.update { it.copy(name = name, nameError = null) } }
    fun onTypeChange(type: CategoryType) { _uiState.update { it.copy(type = type) } }
    fun onColorChange(color: String) { _uiState.update { it.copy(color = color) } }

    // Called by AddEditCategoryScreen save button; sets isSaved=true which triggers onSaved() nav callback
    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Name is required") }
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val category = Category(
                id = categoryId ?: 0L,
                name = state.name.trim(),
                icon = "category",
                color = state.color,
                type = state.type,
                monthlyBudgetInPaise = null,
                isDefault = false,
                isArchived = false
            )
            saveCategory(category)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
