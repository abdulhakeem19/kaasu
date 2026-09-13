package com.kaasu.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.core.datastore.SettingsDataStore
import com.kaasu.app.domain.model.Category
import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.domain.repository.CategoryRepository
import com.kaasu.app.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class CategorySpend(
    val category: Category?,
    val amountInPaise: Long
)

data class DashboardUiState(
    val monthLabel: String = "",
    val totalSpentInPaise: Long = 0L,
    val totalIncomeInPaise: Long = 0L,
    val todaySpentInPaise: Long = 0L,
    val monthlyBudgetInPaise: Long = 0L,
    val weekSpentInPaise: Long = 0L,
    val weekSparkline: List<Long> = emptyList(),
    val dailyAvgInPaise: Long = 0L,
    val dailyAvgDeltaPct: Int = 0,
    val prevMonthLabel: String = "",
    val categoryBreakdown: List<CategorySpend> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val categoryMap: Map<Long, Category> = emptyMap(),
    val displayName: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val zone = ZoneId.systemDefault()
    private val today: LocalDate = LocalDate.now()

    private val monthStart = today.withDayOfMonth(1)
        .atStartOfDay(zone).toInstant().toEpochMilli()
    private val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
        .atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
    private val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
    private val todayEnd = today.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

    // Previous month — used for the daily-average comparison ("12% below May")
    private val prevMonth: LocalDate = today.minusMonths(1)
    private val prevMonthStart = prevMonth.withDayOfMonth(1)
        .atStartOfDay(zone).toInstant().toEpochMilli()
    private val prevMonthEnd = prevMonth.withDayOfMonth(prevMonth.lengthOfMonth())
        .atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

    // Observed by DashboardScreen; reacts to new auto-captured transactions in real time
    val uiState = combine(
        transactionRepository.getByDateRange(monthStart, monthEnd),
        transactionRepository.getByDateRange(prevMonthStart, prevMonthEnd),
        categoryRepository.getAllActive(),
        settingsDataStore.monthlyBudgetInPaise,
        settingsDataStore.displayName
    ) { transactions, prevMonthTx, categories, budget, name ->
        buildState(transactions, prevMonthTx, categories, budget, name)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    private fun isExpense(t: Transaction) =
        t.type == TransactionType.EXPENSE || t.type == TransactionType.TRANSFER

    private fun buildState(
        transactions: List<Transaction>,
        prevMonthTx: List<Transaction>,
        categories: List<Category>,
        budget: Long,
        displayName: String
    ): DashboardUiState {
        val categoryMap = categories.associateBy { it.id }

        val expenses = transactions.filter(::isExpense)
        val incomeTransactions = transactions.filter {
            it.type == TransactionType.INCOME ||
            it.type == TransactionType.CASHBACK ||
            it.type == TransactionType.REFUND
        }

        val totalSpent = expenses.sumOf { it.amountInPaise }
        val totalIncome = incomeTransactions.sumOf { it.amountInPaise }

        val todaySpent = expenses
            .filter { it.transactionTime in todayStart..todayEnd }
            .sumOf { it.amountInPaise }

        // Last 7 calendar days (oldest → newest) for the "This week" card + sparkline.
        // Drawn from both months so the window is correct across a month boundary.
        val allExpenses = expenses + prevMonthTx.filter(::isExpense)
        val weekSparkline = (6 downTo 0).map { offset ->
            val day = today.minusDays(offset.toLong())
            val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = day.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
            allExpenses.filter { it.transactionTime in start..end }.sumOf { it.amountInPaise }
        }
        val weekSpent = weekSparkline.sum()

        // Daily average this month vs last month's daily average.
        val daysElapsed = today.dayOfMonth
        val dailyAvg = if (daysElapsed > 0) totalSpent / daysElapsed else 0L
        val prevSpent = prevMonthTx.filter(::isExpense).sumOf { it.amountInPaise }
        val prevDailyAvg = prevSpent / prevMonth.lengthOfMonth()
        val deltaPct = if (prevDailyAvg > 0)
            (((dailyAvg - prevDailyAvg).toDouble() / prevDailyAvg) * 100).toInt()
        else 0

        val categoryBreakdown = expenses
            .groupBy { it.categoryId }
            .map { (catId, txns) ->
                CategorySpend(
                    category = catId?.let { categoryMap[it] },
                    amountInPaise = txns.sumOf { it.amountInPaise }
                )
            }
            .sortedByDescending { it.amountInPaise }
            .take(5)

        val recentTransactions = transactions
            .sortedByDescending { it.transactionTime }
            .take(5)

        val monthLabel = today.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val prevMonthLabel = prevMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())

        return DashboardUiState(
            monthLabel = monthLabel,
            totalSpentInPaise = totalSpent,
            totalIncomeInPaise = totalIncome,
            todaySpentInPaise = todaySpent,
            monthlyBudgetInPaise = budget,
            weekSpentInPaise = weekSpent,
            weekSparkline = weekSparkline,
            dailyAvgInPaise = dailyAvg,
            dailyAvgDeltaPct = deltaPct,
            prevMonthLabel = prevMonthLabel,
            categoryBreakdown = categoryBreakdown,
            recentTransactions = recentTransactions,
            categoryMap = categoryMap,
            displayName = displayName,
            isLoading = false
        )
    }
}
