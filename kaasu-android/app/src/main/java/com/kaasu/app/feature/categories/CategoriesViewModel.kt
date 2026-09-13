package com.kaasu.app.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.domain.model.Category
import com.kaasu.app.domain.model.CategoryType
import com.kaasu.app.domain.usecase.category.ArchiveCategoryUseCase
import com.kaasu.app.domain.usecase.transaction.GetAllCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryGroup(val type: CategoryType, val categories: List<Category>)

data class CategoriesUiState(
    val groups: List<CategoryGroup> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    getCategories: GetAllCategoriesUseCase,
    private val archiveCategory: ArchiveCategoryUseCase,
) : ViewModel() {

    // Observed by CategoriesScreen via collectAsStateWithLifecycle(); groups categories into EXPENSE/INCOME/SYSTEM sections
    val uiState = getCategories()
        .map { categories ->
            val groups = CategoryType.entries
                .mapNotNull { type ->
                    val list = categories.filter { it.type == type }
                    if (list.isEmpty()) null else CategoryGroup(type, list)
                }
            CategoriesUiState(groups = groups, isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategoriesUiState()
        )

    // Called from CategoryRow archive button in CategoriesScreen; soft-hides the category (isArchived=true) without deleting rules
    fun archive(id: Long) {
        viewModelScope.launch { archiveCategory(id) }
    }
}
