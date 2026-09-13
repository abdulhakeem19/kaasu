package com.kaasu.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.domain.repository.CategoryRepository
import com.kaasu.app.domain.repository.RuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MerchantRuleRow(
    val id: Long,
    val matchText: String,
    val categoryName: String,
    val isSystem: Boolean
)

@HiltViewModel
class MerchantRulesViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val rules = combine(
        ruleRepository.getActiveByPriority(),
        categoryRepository.getAllActive()
    ) { rules, categories ->
        val categoryNames = categories.associate { it.id to it.name }
        rules
            .filter { it.categoryId != null }
            .map {
                MerchantRuleRow(
                    id = it.id,
                    matchText = it.matchText,
                    categoryName = categoryNames[it.categoryId] ?: "Uncategorized",
                    isSystem = it.isSystem
                )
            }
            // User rules (highest priority) first, then seeded ones.
            .sortedByDescending { !it.isSystem }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteRule(id: Long) {
        viewModelScope.launch { ruleRepository.delete(id) }
    }
}
