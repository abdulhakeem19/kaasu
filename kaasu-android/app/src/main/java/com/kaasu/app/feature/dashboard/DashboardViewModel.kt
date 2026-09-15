package com.kaasu.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.core.datastore.SettingsDataStore
import com.kaasu.app.core.util.BudgetCycle
import com.kaasu.app.domain.model.Account
import com.kaasu.app.domain.model.Category
import com.kaasu.app.domain.money.SpendRules
import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.repository.AccountRepository
import com.kaasu.app.domain.repository.CategoryRepository
import com.kaasu.app.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
    // Needed to name both ends of a transfer — "Union Bank → SBI Card" is derived, not stored.
    val accountMap: Map<Long, Account> = emptyMap(),
    val displayName: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
    settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val zone = ZoneId.systemDefault()

    /**
     * The window is re-derived on every emission rather than cached in a field.
     *
     * These bounds used to be `private val`s computed once at construction, so a view model that
     * outlived midnight kept showing the previous day as "Today" and the previous month's window
     * for as long as the process lived.
     *
     * The cycle comes from the owner's month-start day, the same source Budgets already uses. With
     * a start day other than the 1st the two screens previously disagreed about what "this month"
     * meant while comparing against the same budget figure.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState = settingsDataStore.monthStartDay.flatMapLatest { startDay ->
        val today = LocalDate.now()
        val cycle = BudgetCycle.current(startDay, today)
        val previousCycleStart = BudgetCycle.current(startDay, today.minusMonths(1))

        // combine() tops out at five flows and the accounts make six, so the first five are folded
        // into one value and the accounts joined onto that.
        combine(
            transactionRepository.getByDateRange(cycle.startMillis, cycle.endMillis),
            transactionRepository.getByDateRange(previousCycleStart.startMillis, previousCycleStart.endMillis),
            categoryRepository.getAllActive(),
            settingsDataStore.monthlyBudgetInPaise,
            settingsDataStore.displayName
        ) { transactions, prevMonthTx, categories, budget, name ->
            Inputs(transactions, prevMonthTx, categories, budget, name)
        }.combine(accountRepository.getAll()) { inputs, accounts ->
            buildState(inputs, accounts, today, cycle, previousCycleStart)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    /** The five flows combine() can take in one go, so the accounts can be joined on after. */
    private data class Inputs(
        val transactions: List<Transaction>,
        val prevMonthTx: List<Transaction>,
        val categories: List<Category>,
        val budget: Long,
        val displayName: String,
    )

    private fun buildState(
        inputs: Inputs,
        accounts: List<Account>,
        today: LocalDate,
        cycle: BudgetCycle,
        previousCycle: BudgetCycle,
    ): DashboardUiState {
        val (transactions, prevMonthTx, categories, budget, displayName) = inputs
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val todayEnd = today.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
        val categoryMap = categories.associateBy { it.id }

        val expenses = transactions.filter(SpendRules::isSpend)

        // Net of refunds — money handed back is not income, it undoes a purchase.
        val totalSpent = SpendRules.netSpendInPaise(transactions)
        val totalIncome = SpendRules.totalIncomeInPaise(transactions)

        val todaySpent = SpendRules.netSpendInPaise(
            transactions.filter { it.transactionTime in todayStart..todayEnd }
        )

        // Last 7 calendar days (oldest → newest) for the "This week" card + sparkline.
        // Both cycles are pooled so the window stays correct across a cycle boundary, and returns
        // are kept in the pool so a refund reduces its day the same way it reduces the month.
        val recentPool = transactions + prevMonthTx
        val weekSparkline = (6 downTo 0).map { offset ->
            val day = today.minusDays(offset.toLong())
            val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = day.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
            SpendRules.netSpendInPaise(recentPool.filter { it.transactionTime in start..end })
        }
        val weekSpent = weekSparkline.sum()

        // Daily average this month vs last month's daily average.
        // Days into the cycle, not the calendar month — they differ whenever the month-start day
        // is not the 1st, and dividing this cycle's spend by a calendar day count would be wrong.
        val cycleDays = ((cycle.endMillis - cycle.startMillis) / 86_400_000L).toInt() + 1
        val daysElapsed = (((System.currentTimeMillis() - cycle.startMillis) / 86_400_000L) + 1)
            .toInt().coerceIn(1, cycleDays)
        val dailyAvg = totalSpent / daysElapsed
        val prevSpent = SpendRules.netSpendInPaise(prevMonthTx)
        val prevCycleDays =
            ((previousCycle.endMillis - previousCycle.startMillis) / 86_400_000L).toInt() + 1
        val prevDailyAvg = prevSpent / prevCycleDays
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

        val monthLabel = cycle.label
        val prevMonthLabel = previousCycle.label

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
            accountMap = accounts.associateBy { it.id },
            displayName = displayName,
            isLoading = false
        )
    }
}
