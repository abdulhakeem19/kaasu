package com.kaasu.app.feature.reports

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaasu.app.ui.theme.KaasuColors
import com.kaasu.app.core.util.formatRupees
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Palette constants used only in this screen ───────────────────────────────
// Composable getters, not plain vals: a top-level val is initialised once, outside composition,
// which is how this screen's colors stayed pinned to the light theme.
private val DarkGreen: Color
    @Composable @ReadOnlyComposable get() = KaasuColors.forest
private val SavedGreen     = Color(0xFFB8E6C0)                // light green for SAVED
private val OnDarkCreamy   = Color(0xFFF5EFD9)                // cream on dark bg
private val DividerOnDark  = Color(0x33F5EFD9)                // rgba(245,239,217,0.2)

// Heatmap intensity colours (light → dark)
private val HeatEmpty      = Color(0xFFECE3C4)
private val HeatL1         = Color(0xFFD8E2CF)
private val HeatL2         = Color(0xFFB0C6B3)
private val HeatL3         = Color(0xFF6F9889)
private val HeatL4         = Color(0xFF0F4A37)

// Donut chart segment colors
private val segmentColors: List<Color>
    @Composable @ReadOnlyComposable get() = listOf(
    KaasuColors.forest,
    Color(0xFF8B7355),
    KaasuColors.expense,
    Color(0xFF3B7A4F),
    Color(0xFF6B4E9B),
    Color(0xFF9B4E4E)
)

@Composable
fun ReportsScreen(
    onNavigateToBudgets: () -> Unit = {},
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.exportEvent.collect { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export transactions"))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { viewModel.exportCsv(context) },
                    containerColor = DarkGreen,
                    contentColor = OnDarkCreamy
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export CSV")
                }
            } else {
                FloatingActionButton(
                    onClick = onNavigateToBudgets,
                    containerColor = DarkGreen,
                    contentColor = OnDarkCreamy
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Budgets")
                }
            }
        }
    ) { innerPadding ->

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DarkGreen)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = KaasuColors.forest
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Breakdown",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Monthly",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            // Content
            if (selectedTab == 0) {
                BreakdownContent(state = state)
            } else {
                MonthlyContent(state = state, viewModel = viewModel, context = context)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Breakdown tab
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BreakdownContent(state: ReportsUiState) {
    val breakdown = state.categoryBreakdown
    val total = breakdown.sumOf { it.amountInPaise }

    if (total == 0L) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No expenses this month",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Month nav header
        item {
            ReportsAppBar(
                label = state.selectedMonthLabel,
                canGoForward = state.canGoForward,
                onPrevious = {},
                onNext = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }

        // Donut chart
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 8.dp)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                // Read out of composition before entering the DrawScope, which cannot.
                val donutColors = segmentColors
                val donutFallback = KaasuColors.forest
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = size.minDimension * 0.18f
                    val radius = (size.minDimension - strokeWidth) / 2f
                    val topLeft = Offset(
                        x = center.x - radius,
                        y = center.y - radius
                    )
                    val arcSize = Size(radius * 2, radius * 2)
                    var startAngle = -90f
                    val gapDeg = 2f

                    breakdown.forEachIndexed { index, item ->
                        val fraction = item.amountInPaise.toFloat() / total.toFloat()
                        val sweepAngle = fraction * (360f - gapDeg * breakdown.size)
                        val color = donutColors.getOrElse(index) { donutFallback }
                        drawArc(
                            color = color,
                            startAngle = startAngle + gapDeg / 2,
                            sweepAngle = sweepAngle - gapDeg,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth)
                        )
                        startAngle += sweepAngle + gapDeg
                    }
                }
                // Center text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOTAL · ${state.selectedMonthLabel.split(" ").firstOrNull()?.take(3)?.uppercase() ?: ""}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = total.formatRupees(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Section label
        item {
            Text(
                text = "BY CATEGORY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        // Category list
        itemsIndexed(breakdown) { index, item ->
            val color = segmentColors.getOrElse(index) { KaasuColors.forest }
            val percent = if (total > 0) (item.amountInPaise * 100 / total).toInt() else 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color, CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = item.category?.name ?: "Uncategorized",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = item.amountInPaise.formatRupees(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$percent%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.End
                )
            }
            if (index < breakdown.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(horizontal = 44.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Monthly tab
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonthlyContent(
    state: ReportsUiState,
    viewModel: ReportsViewModel,
    context: Context
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            ReportsAppBar(
                label = state.selectedMonthLabel,
                canGoForward = state.canGoForward,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
        item {
            SummaryCard(
                incomeInPaise = state.totalIncomeInPaise,
                spentInPaise = state.totalSpentInPaise,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
        item {
            DailySpendHeatmap(
                monthLabel = state.selectedMonthLabel,
                dailySpendMap = state.dailySpendMap,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(top = 24.dp)
            )
        }
        item {
            InsightsSection(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 18.dp)
            )
        }
        if (state.sixMonthTrend.any { it.amountInPaise > 0 }) {
            item {
                TrendSection(
                    data = state.sixMonthTrend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 18.dp)
                )
            }
        }
        if (state.transactionCount == 0) {
            item {
                Text(
                    text = "No transactions this month",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// App bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReportsAppBar(
    label: String,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left chevron button
        NavChevronButton(
            onClick = onPrevious,
            enabled = true,
            contentDescription = "Previous month",
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft
        )

        // Center two-line title
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "MONTHLY REPORT",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Right chevron button
        NavChevronButton(
            onClick = onNext,
            enabled = canGoForward,
            contentDescription = "Next month",
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight
        )
    }
}

@Composable
private fun NavChevronButton(
    onClick: () -> Unit,
    enabled: Boolean,
    contentDescription: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) MaterialTheme.colorScheme.onSurface
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Summary card (dark green)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(
    incomeInPaise: Long,
    spentInPaise: Long,
    modifier: Modifier = Modifier
) {
    val savedInPaise = incomeInPaise - spentInPaise

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(DarkGreen)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryStatColumn(
                label = "IN",
                value = incomeInPaise.formatRupees(),
                valueColor = OnDarkCreamy,
                modifier = Modifier.weight(1f)
            )

            // Thin vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(44.dp)
                    .background(DividerOnDark)
            )

            SummaryStatColumn(
                label = "OUT",
                value = spentInPaise.formatRupees(),
                valueColor = OnDarkCreamy,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(44.dp)
                    .background(DividerOnDark)
            )

            SummaryStatColumn(
                label = "SAVED",
                value = if (savedInPaise >= 0) savedInPaise.formatRupees()
                        else "-${(-savedInPaise).formatRupees()}",
                valueColor = if (savedInPaise >= 0) SavedGreen else Color(0xFFEF8080),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryStatColumn(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = OnDarkCreamy.copy(alpha = 0.7f),
            letterSpacing = 1.sp
        )
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Daily spend heatmap
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DailySpendHeatmap(
    monthLabel: String,
    dailySpendMap: Map<Int, Long>,
    modifier: Modifier = Modifier
) {
    // Derive YearMonth from the label ("May 2026" format)
    val yearMonth: YearMonth = remember(monthLabel) {
        runCatching {
            YearMonth.parse(
                monthLabel,
                DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
            )
        }.getOrDefault(YearMonth.now())
    }

    val today = LocalDate.now()
    val daysInMonth = yearMonth.lengthOfMonth()

    // Weekday offset so we start on Monday (ISO week)
    val firstDayOfMonth = yearMonth.atDay(1)
    val startOffset = (firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7

    // Compute 4 quantile breakpoints from non-zero spends
    val nonZeroSpends = (1..daysInMonth)
        .mapNotNull { day -> dailySpendMap[day]?.takeIf { it > 0 } }
        .sorted()

    val thresholds: List<Long> = if (nonZeroSpends.size >= 4) {
        listOf(
            nonZeroSpends[nonZeroSpends.size / 4],
            nonZeroSpends[nonZeroSpends.size / 2],
            nonZeroSpends[nonZeroSpends.size * 3 / 4]
        )
    } else if (nonZeroSpends.isNotEmpty()) {
        val max = nonZeroSpends.last()
        listOf(max / 4, max / 2, max * 3 / 4)
    } else {
        listOf(1L, 2L, 3L)
    }

    fun heatColor(day: Int): Color? {
        val spend = dailySpendMap[day] ?: 0L
        if (spend == 0L) return null
        return when {
            spend <= thresholds[0] -> HeatL1
            spend <= thresholds[1] -> HeatL2
            spend <= thresholds[2] -> HeatL3
            else                   -> HeatL4
        }
    }

    val noSpendDays = (1..daysInMonth).count { (dailySpendMap[it] ?: 0L) == 0L }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Section label
        Text(
            text = "DAILY SPEND · ${monthLabel.uppercase(LocalConfiguration.current.locales[0])}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 1.sp
        )

        // Day-of-week header row
        val dayHeaders = listOf("M", "T", "W", "T", "F", "S", "S")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            dayHeaders.forEach { d ->
                Text(
                    text = d,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 5-row grid
        repeat(5) { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(7) { col ->
                    val cellIndex = week * 7 + col
                    val day = cellIndex - startOffset + 1
                    val isValidDay = day in 1..daysInMonth
                    val isFuture = isValidDay && yearMonth.atDay(day).isAfter(today)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(5.dp))
                            .then(
                                when {
                                    !isValidDay -> Modifier.background(Color.Transparent)
                                    isFuture -> Modifier
                                        .background(Color.Transparent)
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(5.dp)
                                        )
                                    else -> {
                                        val color = heatColor(day) ?: HeatEmpty
                                        Modifier.background(color)
                                    }
                                }
                            )
                    )
                }
            }
        }

        // Legend row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Less",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            listOf(HeatEmpty, HeatL1, HeatL2, HeatL3, HeatL4).forEach { c ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(c)
                )
                Spacer(Modifier.width(3.dp))
            }
            Text(
                text = "More",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "$noSpendDays no-spend days",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Insights section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InsightsSection(
    state: ReportsUiState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "INSIGHTS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // Food spend insight card
        val foodBreakdown = state.categoryBreakdown.firstOrNull {
            it.category?.name?.contains("food", ignoreCase = true) == true ||
            it.category?.name?.contains("dining", ignoreCase = true) == true ||
            it.category?.name?.contains("restaurant", ignoreCase = true) == true
        }
        val foodPercent = if (state.totalSpentInPaise > 0 && foodBreakdown != null) {
            (foodBreakdown.amountInPaise * 100 / state.totalSpentInPaise).toInt()
        } else null

        InsightCard(
            chipColor = Color(0xFFD4EDDA),
            chipIconColor = KaasuColors.income,
            chipIcon = "🍽",
            text = if (foodPercent != null)
                "Food & dining is $foodPercent% of your spend this month."
            else
                "No food & dining spend recorded this month."
        )

        // Subscription insight card
        val subscriptionBreakdown = state.categoryBreakdown.firstOrNull {
            it.category?.name?.contains("subscription", ignoreCase = true) == true ||
            it.category?.name?.contains("streaming", ignoreCase = true) == true
        }
        InsightCard(
            chipColor = KaasuColors.amberBg,
            chipIconColor = KaasuColors.amber,
            chipIcon = "🔄",
            text = if (subscriptionBreakdown != null)
                "Subscriptions cost you ${subscriptionBreakdown.amountInPaise.formatRupees()} this month."
            else
                "No subscription payments detected this month."
        )
    }
}

@Composable
private fun InsightCard(
    chipColor: Color,
    chipIconColor: Color,
    chipIcon: String,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Colored icon chip
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(chipColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chipIcon,
                fontSize = 14.sp
            )
        }

        Text(
            text = text,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6-month trend chart
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TrendSection(
    data: List<MonthlyTotal>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "6-MONTH TREND",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 1.sp
        )

        SpendBarChart(data = data)
    }
}

@Composable
private fun SpendBarChart(data: List<MonthlyTotal>, modifier: Modifier = Modifier) {
    val maxAmount = data.maxOfOrNull { it.amountInPaise }?.takeIf { it > 0 } ?: 1L
    val barColor = DarkGreen.copy(alpha = 0.55f)
    val highlightColor = DarkGreen

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEachIndexed { index, item ->
            val fraction = (item.amountInPaise.toFloat() / maxAmount).coerceAtLeast(
                if (item.amountInPaise > 0) 0.04f else 0f
            )
            val isLast = index == data.lastIndex
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height((100 * fraction).dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (isLast) highlightColor else barColor)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.label,
                    fontSize = 10.sp,
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                    color = if (isLast) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
