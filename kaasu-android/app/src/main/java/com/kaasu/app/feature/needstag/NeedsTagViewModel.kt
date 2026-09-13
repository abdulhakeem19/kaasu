package com.kaasu.app.feature.needstag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.domain.model.Category
import com.kaasu.app.domain.model.MatchType
import com.kaasu.app.domain.model.Rule
import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.repository.RuleRepository
import com.kaasu.app.domain.repository.TransactionRepository
import com.kaasu.app.domain.usecase.transaction.GetAllCategoriesUseCase
import com.kaasu.app.domain.usecase.transaction.SuggestCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TagItem(
    val transaction: Transaction,
    val suggestedCategoryId: Long?
)

data class NeedsTagUiState(
    val items: List<TagItem> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true
) {
    val categoryMap: Map<Long, Category> get() = categories.associateBy { it.id }
}

@HiltViewModel
class NeedsTagViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val ruleRepository: RuleRepository,
    private val getCategories: GetAllCategoriesUseCase,
    private val suggestCategory: SuggestCategoryUseCase,
) : ViewModel() {

    // Locally-skipped ids ("skip for now" doesn't change the DB, so we filter them out here).
    private val skipped = MutableStateFlow<Set<Long>>(emptySet())

    // Observe the uncategorized queue live: tagging an item (here or anywhere) gives it a category
    // so it drops out of the flow immediately, and newly-captured untagged transactions appear.
    val uiState = combine(
        transactionRepository.getUncategorized(),
        getCategories(),
        skipped
    ) { uncategorized, categories, skippedIds ->
        val items = uncategorized
            .filterNot { it.id in skippedIds }
            .map { tx ->
                TagItem(
                    transaction = tx,
                    suggestedCategoryId = suggestCategory(tx.merchantName, tx.sourceAppPackage, categories)
                )
            }
        NeedsTagUiState(items = items, categories = categories, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NeedsTagUiState()
    )

    /** Apply a category to one transaction; optionally remember it as a rule for this merchant. */
    fun tag(transactionId: Long, categoryId: Long, alwaysForMerchant: Boolean) {
        viewModelScope.launch {
            if (alwaysForMerchant) {
                uiState.value.items.firstOrNull { it.transaction.id == transactionId }
                    ?.transaction?.merchantName?.takeIf { it.isNotBlank() }
                    ?.let { merchant -> upsertMerchantRule(merchant, categoryId) }
            }
            // Setting a category drops it from getUncategorized() → it leaves the list automatically.
            transactionRepository.setCategory(transactionId, categoryId)
        }
    }

    /** Accept every suggestion in one go. */
    fun confirmAllSuggestions() {
        viewModelScope.launch {
            uiState.value.items
                .filter { it.suggestedCategoryId != null }
                .forEach { transactionRepository.setCategory(it.transaction.id, it.suggestedCategoryId!!) }
        }
    }

    fun skip(transactionId: Long) {
        skipped.value = skipped.value + transactionId
    }

    // High-priority user rule so future captures of this merchant auto-tag to the chosen category.
    private suspend fun upsertMerchantRule(merchant: String, categoryId: Long) {
        ruleRepository.insert(
            Rule(
                name = merchant,
                matchText = merchant,
                matchType = MatchType.CONTAINS,
                categoryId = categoryId,
                transactionType = null,
                sourceAppPackage = null,
                priority = 100, // above seeded system rules (priority 10)
                isSystem = false,
                isActive = true
            )
        )
    }
}
