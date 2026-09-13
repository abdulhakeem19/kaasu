package com.kaasu.app.feature.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.core.datastore.SettingsDataStore
import com.kaasu.app.core.util.BudgetCycle
import com.kaasu.app.domain.model.Category
import com.kaasu.app.domain.model.CategoryType
import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.domain.repository.CategoryRepository
import com.kaasu.app.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CategoryBudget(
    val category: Category,
    val spentInPaise: Long,
    val budgetInPaise: Long
) {
    val progress: Float get() = if (budgetInPaise > 0) (spentInPaise.toFloat() / budgetInPaise).coerceIn(0f, 1f) else 0f
    val remainingInPaise: Long get() = budgetInPaise - spentInPaise
    val isOver: Boolean get() = spentInPaise > budgetInPaise
    val overByPaise: Long get() = (spentInPaise - budgetInPaise).coerceAtLeast(0)
    val overByPct: Int get() = if (budgetInPaise > 0) (((spentInPaise - budgetInPaise).toFloat() / budgetInPaise) * 100).toInt() else 0
}

data class BudgetsUiState(
    val monthLabel: String = "",
    val overallSpentInPaise: Long = 0L,
    val overallBudgetInPaise: Long = 0L,
    val projectedInPaise: Long = 0L,
    val categoryBudgets: List<CategoryBudget> = emptyList(),
    // Expense categories with no limit set — offered for "add a limit".
    val unbudgetedCategories: List<Category> = emptyList(),
    val isLoading: Boolean = true
) {
    val overallProgress: Float get() = if (overallBudgetInPaise > 0) (overallSpentInPaise.toFloat() / overallBudgetInPaise).coerceIn(0f, 1f) else 0f
    val overallPct: Int get() = if (overallBudgetInPaise > 0) ((overallSpentInPaise.toFloat() / overallBudgetInPaise) * 100).toInt() else 0
}

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    settingsDataStore: SettingsDataStore,
) : ViewModel() {

    // Set (paise>0) or clear (paise<=0) a category's monthly limit.
    fun setCategoryBudget(category: Category, paise: Long) {
        viewModelScope.launch {
            categoryRepository.update(category.copy(monthlyBudgetInPaise = paise.takeIf { it > 0 }))
        }
    }

    private val today: LocalDate = LocalDate.now()

    // The budget cycle window depends on the user's month-start day, so the transaction query is
    // re-derived whenever that setting changes.
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState = settingsDataStore.monthStartDay.flatMapLatest { startDay ->
        val cycle = BudgetCycle.current(startDay, today)
        combine(
            transactionRepository.getByDateRange(cycle.startMillis, cycle.endMillis),
            categoryRepository.getAllActive(),
            settingsDataStore.monthlyBudgetInPaise
        ) { transactions, categories, overallBudget ->
            buildState(transactions, categories, overallBudget, cycle)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BudgetsUiState()
    )

    private fun buildState(
        transactions: List<com.kaasu.app.domain.model.Transaction>,
        categories: List<Category>,
        overallBudget: Long,
        cycle: BudgetCycle
    ): BudgetsUiState {
        val expenses = transactions.filter {
            it.type == TransactionType.EXPENSE || it.type == TransactionType.TRANSFER
        }
        val overallSpent = expenses.sumOf { it.amountInPaise }

        val spendByCategory = expenses
            .groupBy { it.categoryId }
            .mapValues { (_, txns) -> txns.sumOf { it.amountInPaise } }

        // Budgeted categories first (biggest spend first); the rest are offered to add a limit.
        val categoryBudgets = categories
            .filter { (it.monthlyBudgetInPaise ?: 0L) > 0L }
            .map { cat ->
                CategoryBudget(
                    category = cat,
                    spentInPaise = spendByCategory[cat.id] ?: 0L,
                    budgetInPaise = cat.monthlyBudgetInPaise ?: 0L
                )
            }
            .sortedByDescending { it.spentInPaise }

        val unbudgeted = categories
            .filter { (it.monthlyBudgetInPaise ?: 0L) <= 0L && it.type == CategoryType.EXPENSE }
            .sortedBy { it.name }

        // Linear projection across the cycle: current daily burn × days in the cycle.
        val cycleDays = ((cycle.endMillis - cycle.startMillis) / 86_400_000L).toInt() + 1
        val daysElapsed = (((System.currentTimeMillis() - cycle.startMillis) / 86_400_000L).toInt() + 1).coerceIn(1, cycleDays)
        val projected = (overallSpent.toDouble() / daysElapsed * cycleDays).toLong()

        return BudgetsUiState(
            monthLabel = cycle.label,
            overallSpentInPaise = overallSpent,
            overallBudgetInPaise = overallBudget,
            projectedInPaise = projected,
            categoryBudgets = categoryBudgets,
            unbudgetedCategories = unbudgeted,
            isLoading = false
        )
    }
}
