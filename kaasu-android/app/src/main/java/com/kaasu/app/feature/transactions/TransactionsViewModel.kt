package com.kaasu.app.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.domain.model.Account
import com.kaasu.app.domain.model.Category
import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.domain.usecase.account.GetAccountsUseCase
import com.kaasu.app.domain.usecase.transaction.GetAllCategoriesUseCase
import com.kaasu.app.domain.usecase.transaction.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class TransactionGroup(val dateLabel: String, val transactions: List<Transaction>)

// A chip in the filter row. id == null is the "All" chip.
data class CategoryChip(val id: Long?, val label: String)

data class TransactionsUiState(
    val groups: List<TransactionGroup> = emptyList(),
    val categoryMap: Map<Long, Category> = emptyMap(),
    val accountMap: Map<Long, Account> = emptyMap(),
    val chips: List<CategoryChip> = emptyList(),
    val selectedCategoryId: Long? = null,
    val searchQuery: String = "",
    val selectedType: TransactionType? = null,
    val uncategorizedOnly: Boolean = false,
    val isLoading: Boolean = true
) {
    // Drives the filter button's "active" dot. The category chips are visible on screen already,
    // so they are deliberately not counted here — only filters hidden behind the sheet.
    val hasHiddenFilters: Boolean get() = selectedType != null || uncategorizedOnly
}

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    getTransactions: GetTransactionsUseCase,
    getCategories: GetAllCategoriesUseCase,
    getAccounts: GetAccountsUseCase,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _selectedType = MutableStateFlow<TransactionType?>(null)
    private val _uncategorizedOnly = MutableStateFlow(false)

    // The four filters are folded into one flow first: combine() tops out at five sources, and
    // transactions/categories/accounts already take three of them.
    private data class Filters(
        val query: String,
        val categoryId: Long?,
        val type: TransactionType?,
        val uncategorizedOnly: Boolean,
    )

    private val filters = combine(
        _searchQuery, _selectedCategoryId, _selectedType, _uncategorizedOnly
    ) { query, categoryId, type, uncategorizedOnly ->
        Filters(query, categoryId, type, uncategorizedOnly)
    }

    val uiState = combine(
        getTransactions(),
        getCategories(),
        getAccounts(),
        filters
    ) { transactions, categories, accounts, f ->
        val (query, selectedCategoryId, selectedType, uncategorizedOnly) = f
        val categoryMap = categories.associateBy { it.id }
        val accountMap = accounts.associateBy { it.id }
        val filtered = transactions
            .filter { selectedCategoryId == null || it.categoryId == selectedCategoryId }
            .filter { selectedType == null || it.type == selectedType }
            .filter { !uncategorizedOnly || it.categoryId == null }
            .filter { matchesQuery(it, query, categoryMap) }
        TransactionsUiState(
            groups = groupByDate(filtered),
            categoryMap = categoryMap,
            accountMap = accountMap,
            chips = buildChips(transactions, categoryMap),
            selectedCategoryId = selectedCategoryId,
            searchQuery = query,
            selectedType = selectedType,
            uncategorizedOnly = uncategorizedOnly,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionsUiState()
    )

    fun onSearchQueryChange(query: String) { _searchQuery.update { query } }
    fun onCategorySelect(categoryId: Long?) { _selectedCategoryId.update { categoryId } }
    fun onTypeSelect(type: TransactionType?) { _selectedType.update { type } }
    fun onUncategorizedOnlyChange(enabled: Boolean) { _uncategorizedOnly.update { enabled } }

    fun clearFilters() {
        _selectedType.value = null
        _uncategorizedOnly.value = false
    }

    // "All · N" plus the most-used categories present in the data (top 6 by transaction count).
    private fun buildChips(
        transactions: List<Transaction>,
        categoryMap: Map<Long, Category>
    ): List<CategoryChip> {
        val all = CategoryChip(id = null, label = "All · ${transactions.size}")
        val topCategories = transactions
            .mapNotNull { it.categoryId }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .mapNotNull { (catId, _) -> categoryMap[catId]?.let { CategoryChip(catId, it.name) } }
            .take(6)
        return listOf(all) + topCategories
    }

    private fun matchesQuery(
        t: Transaction,
        query: String,
        categoryMap: Map<Long, Category>
    ): Boolean {
        if (query.isBlank()) return true
        val q = query.lowercase()
        return t.merchantName?.lowercase()?.contains(q) == true ||
                t.note?.lowercase()?.contains(q) == true ||
                categoryMap[t.categoryId]?.name?.lowercase()?.contains(q) == true
    }

    private fun groupByDate(transactions: List<Transaction>): List<TransactionGroup> {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val formatter = DateTimeFormatter.ofPattern("EEE, d MMM")
        return transactions
            .groupBy { tx ->
                Instant.ofEpochMilli(tx.transactionTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
            .map { (date, txList) ->
                val label = when (date) {
                    today -> "Today"
                    yesterday -> "Yesterday"
                    else -> date.format(formatter)
                }
                TransactionGroup(dateLabel = label, transactions = txList)
            }
    }
}
