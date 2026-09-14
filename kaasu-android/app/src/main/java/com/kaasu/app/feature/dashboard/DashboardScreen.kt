package com.kaasu.app.feature.dashboard

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaasu.app.ui.theme.KaasuColors
import com.kaasu.app.core.util.formatRupees
import com.kaasu.app.core.util.toComposeColor
import com.kaasu.app.ui.components.TransactionCard
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardScreen(
    onTransactionClick: (Long) -> Unit,
    onSeeAllClick: () -> Unit,
    // The header search button opens the transactions list, which is where search actually lives.
    onSearchClick: () -> Unit = onSeeAllClick,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isNotificationGranted by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNotificationGranted = NotificationManagerCompat
                    .getEnabledListenerPackages(context)
                    .contains(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Header row
        item {
            DashboardHeader(displayName = state.displayName, onSearchClick = onSearchClick)
        }

        // Notification warning banner
        if (!isNotificationGranted) {
            item {
                NotificationBanner(
                    onEnableClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // Big spend card
        item {
            SpendCard(
                totalSpentInPaise = state.totalSpentInPaise,
                monthlyBudgetInPaise = state.monthlyBudgetInPaise,
                monthLabel = state.monthLabel,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Stat row
        item {
            StatRow(
                weekSpentInPaise = state.weekSpentInPaise,
                weekSparkline = state.weekSparkline,
                dailyAvgInPaise = state.dailyAvgInPaise,
                dailyAvgDeltaPct = state.dailyAvgDeltaPct,
                prevMonthLabel = state.prevMonthLabel,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp)
            )
        }

        if (state.recentTransactions.isNotEmpty()) {
            // Today section header
            val todayTotal = state.todaySpentInPaise
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (todayTotal > 0) "Today · ${todayTotal.formatRupees()}" else "Today",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = KaasuColors.ink
                    )
                    Text(
                        text = "View all",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = KaasuColors.forest,
                        modifier = Modifier.clickable(onClick = onSeeAllClick)
                    )
                }
            }

            items(state.recentTransactions, key = { it.id }) { tx ->
                TransactionCard(
                    transaction = tx,
                    category = tx.categoryId?.let { state.categoryMap[it] },
                    onClick = { onTransactionClick(tx.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
        } else {
            item {
                EmptyState(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 48.dp)
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(displayName: String, onSearchClick: () -> Unit) {
    val today = LocalDate.now()
    val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, LocalConfiguration.current.locales[0])
        .uppercase()
    val firstName = displayName.trim().substringBefore(' ').ifBlank { null }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD8E2CF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = firstName?.firstOrNull()?.uppercase() ?: "K",
                    color = KaasuColors.forest,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }
            Column {
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = KaasuColors.muted
                )
                Text(
                    text = if (firstName != null) "Hi, $firstName" else "Hi there",
                    style = MaterialTheme.typography.titleMedium,
                    color = KaasuColors.ink
                )
            }
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, KaasuColors.border, RoundedCornerShape(12.dp))
                .clickable(onClick = onSearchClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                modifier = Modifier.size(16.dp),
                tint = KaasuColors.ink
            )
        }
    }
}

@Composable
private fun SpendCard(
    totalSpentInPaise: Long,
    monthlyBudgetInPaise: Long,
    monthLabel: String,
    modifier: Modifier = Modifier
) {
    val progress = if (monthlyBudgetInPaise > 0)
        (totalSpentInPaise.toFloat() / monthlyBudgetInPaise).coerceIn(0f, 1f)
    else 0f
    val pct = (progress * 100).toInt()
    val remaining = monthlyBudgetInPaise - totalSpentInPaise

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(KaasuColors.forest)
            .padding(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent · $monthLabel".uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp,
                    color = KaasuColors.onForest.copy(alpha = 0.75f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ALL ACCOUNTS",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.sp,
                        color = KaasuColors.onForest.copy(alpha = 0.75f)
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = KaasuColors.onForest.copy(alpha = 0.75f)
                    )
                }
            }

            // Big amount
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "₹",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KaasuColors.onForest.copy(alpha = 0.7f),
                    lineHeight = 50.sp
                )
                Text(
                    text = totalSpentInPaise.formatRupees().removePrefix("₹"),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-2).sp,
                    color = KaasuColors.onForest,
                    lineHeight = 50.sp
                )
            }

            if (monthlyBudgetInPaise > 0) {
                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(KaasuColors.onForest.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(KaasuColors.onForest)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$pct% of budget",
                        style = MaterialTheme.typography.labelSmall,
                        color = KaasuColors.onForest.copy(alpha = 0.7f)
                    )
                    if (remaining >= 0) {
                        Text(
                            text = "${remaining.formatRupees()} left of ${monthlyBudgetInPaise.formatRupees()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = KaasuColors.onForest
                        )
                    } else {
                        Text(
                            text = "Over by ${(-remaining).formatRupees()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB3B3)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(
    weekSpentInPaise: Long,
    weekSparkline: List<Long>,
    dailyAvgInPaise: Long,
    dailyAvgDeltaPct: Int,
    prevMonthLabel: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // This week + sparkline
        StatCard(
            label = "This week",
            value = weekSpentInPaise.formatRupees(),
            modifier = Modifier.weight(1f)
        ) {
            Sparkline(
                points = weekSparkline,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            )
        }
        // Daily average + comparison pill
        StatCard(
            label = "Daily avg",
            value = dailyAvgInPaise.formatRupees(),
            modifier = Modifier.weight(1f)
        ) {
            if (dailyAvgDeltaPct != 0 && prevMonthLabel.isNotEmpty()) {
                val below = dailyAvgDeltaPct < 0
                Text(
                    text = "${if (below) "▾" else "▴"} ${kotlin.math.abs(dailyAvgDeltaPct)}% " +
                        "${if (below) "below" else "above"} $prevMonthLabel",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (below) KaasuColors.income else KaasuColors.expense
                )
            } else {
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = KaasuColors.ink,
    footer: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, KaasuColors.border, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = KaasuColors.muted
            )
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                letterSpacing = (-0.5).sp,
                color = valueColor
            )
            footer()
        }
    }
}

@Composable
private fun Sparkline(points: List<Long>, modifier: Modifier = Modifier) {
    // Canvas draws inside a DrawScope, which is not composition, so the palette is read out here.
    val strokeColor = KaasuColors.forest
    // A day whose refunds outweighed its spending nets below zero. That is a true figure and the
    // "This week" total keeps it, but a line has nowhere below the baseline to go, so it is
    // flattened here at the point of drawing rather than in the state.
    val drawn = points.map { it.coerceAtLeast(0L) }
    val max = (drawn.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Canvas(modifier = modifier) {
        if (drawn.size < 2) return@Canvas
        val stepX = size.width / (drawn.size - 1)
        val path = Path()
        drawn.forEachIndexed { i, v ->
            val x = stepX * i
            val y = size.height - (v.toFloat() / max) * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun NotificationBanner(onEnableClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFF0F0))
            .border(1.dp, KaasuColors.expense.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable(onClick = onEnableClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Notification access disabled",
                style = MaterialTheme.typography.labelLarge,
                color = KaasuColors.expense
            )
            Text(
                text = "Kaasu can't auto-capture transactions without it. Tap to enable.",
                style = MaterialTheme.typography.labelSmall,
                color = KaasuColors.expense.copy(alpha = 0.8f)
            )
        }
        Text(
            text = "Enable",
            style = MaterialTheme.typography.labelLarge,
            color = KaasuColors.expense
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "No transactions yet",
            style = MaterialTheme.typography.titleMedium,
            color = KaasuColors.muted,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Grant notification access and Kaasu will auto-capture payments from your UPI apps and banks.",
            style = MaterialTheme.typography.bodySmall,
            color = KaasuColors.muted,
            textAlign = TextAlign.Center
        )
    }
}
