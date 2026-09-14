package com.kaasu.app.feature.reports

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.domain.money.SpendRules
import com.kaasu.app.core.export.CsvExporter
import com.kaasu.app.domain.model.Category
import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.domain.repository.CategoryRepository
import com.kaasu.app.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class MonthlyTotal(val label: String, val amountInPaise: Long)

data class ReportCategorySpend(val category: Category?, val amountInPaise: Long)

data class ReportsUiState(
    val selectedMonthLabel: String = "",
    val canGoForward: Boolean = false,
    val totalSpentInPaise: Long = 0L,
    val totalIncomeInPaise: Long = 0L,
    val transactionCount: Int = 0,
    val autoCount: Int = 0,
    val categoryBreakdown: List<ReportCategorySpend> = emptyList(),
    val sixMonthTrend: List<MonthlyTotal> = emptyList(),
    /** day-of-month (1–31) → total expense paise for that day in the selected month */
    val dailySpendMap: Map<Int, Long> = emptyMap(),
    val isLoading: Boolean = true,
    val isExporting: Boolean = false
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val csvExporter: CsvExporter,
) : ViewModel() {

    private val zone = ZoneId.systemDefault()
    private val _selectedMonth = MutableStateFlow(YearMonth.now())

    // Observed by ReportsScreen to launch the system share sheet
    private val _exportEvent = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
    val exportEvent: SharedFlow<Uri> = _exportEvent.asSharedFlow()

    val uiState = combine(
        transactionRepository.getAll(),
        categoryRepository.getAllActive(),
        _selectedMonth
    ) { allTransactions, categories, selectedMonth ->
        buildState(allTransactions, categories, selectedMonth)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReportsUiState()
    )

    fun previousMonth() { _selectedMonth.update { it.minusMonths(1) } }

    // Prevents navigating past the current month
    fun nextMonth() {
        _selectedMonth.update { current ->
            if (current < YearMonth.now()) current.plusMonths(1) else current
        }
    }

    // Exports ALL transactions (not just selected month) so the CSV is a full data backup
    fun exportCsv(context: Context) {
        viewModelScope.launch {
            val allTransactions = transactionRepository.getAll().first()
            val categories = categoryRepository.getAllActive().first()
            val categoryMap = categories.associateBy { it.id }
            val file = csvExporter.export(allTransactions, categoryMap)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            _exportEvent.emit(uri)
        }
    }

    private fun buildState(
        allTransactions: List<Transaction>,
        categories: List<Category>,
        selectedMonth: YearMonth
    ): ReportsUiState {
        val categoryMap = categories.associateBy { it.id }
        val now = YearMonth.now()

        val (monthStart, monthEnd) = selectedMonth.toMilliRange()
        val monthTransactions = allTransactions.filter {
            it.transactionTime in monthStart..monthEnd
        }

        val expenses = monthTransactions.filter {
            SpendRules.isSpend(it)
        }
        val income = monthTransactions.filter {
            // Refunds and cashback are no longer income: counting money back as earnings
            // overstated both sides of the month. They reduce spend instead — see SpendRules.
            SpendRules.isIncome(it)
        }

        val categoryBreakdown = expenses
            .groupBy { it.categoryId }
            .map { (catId, txns) ->
                ReportCategorySpend(
                    category = catId?.let { categoryMap[it] },
                    amountInPaise = txns.sumOf { it.amountInPaise }
                )
            }
            .sortedByDescending { it.amountInPaise }
            .take(6)

        // Last 6 months ending at selectedMonth, computed in-memory from full transaction list
        val sixMonthTrend = (5 downTo 0).map { monthsBack ->
            val m = selectedMonth.minusMonths(monthsBack.toLong())
            val (s, e) = m.toMilliRange()
            val spent = allTransactions
                .filter { it.transactionTime in s..e }
                .filter(SpendRules::isSpend)
                .sumOf { it.amountInPaise }
            MonthlyTotal(
                label = m.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                amountInPaise = spent
            )
        }

        val monthLabel = selectedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
            " ${selectedMonth.year}"

        // Build day-of-month → total expense paise map for the heatmap
        val dailySpendMap: Map<Int, Long> = expenses
            .groupBy { txn ->
                Instant.ofEpochMilli(txn.transactionTime)
                    .atZone(zone)
                    .toLocalDate()
                    .dayOfMonth
            }
            .mapValues { (_, txns) -> txns.sumOf { it.amountInPaise } }

        return ReportsUiState(
            selectedMonthLabel = monthLabel,
            canGoForward = selectedMonth < now,
            totalSpentInPaise = expenses.sumOf { it.amountInPaise },
            totalIncomeInPaise = income.sumOf { it.amountInPaise },
            transactionCount = monthTransactions.size,
            autoCount = monthTransactions.count { !it.isManual },
            categoryBreakdown = categoryBreakdown,
            sixMonthTrend = sixMonthTrend,
            dailySpendMap = dailySpendMap,
            isLoading = false
        )
    }

    private fun YearMonth.toMilliRange(): Pair<Long, Long> {
        val start = atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
        return start to end
    }
}
