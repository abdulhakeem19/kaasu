package com.kaasu.app.feature.budgets

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaasu.app.ui.theme.KaasuColors
import com.kaasu.app.core.util.formatRupees
import com.kaasu.app.core.util.toComposeColor

@Composable
fun BudgetsScreen(
    onBack: () -> Unit,
    onEditBudgets: () -> Unit,
    viewModel: BudgetsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // The category currently being edited in the set-limit dialog.
    var editing by remember { mutableStateOf<com.kaasu.app.domain.model.Category?>(null) }

    editing?.let { cat ->
        SetLimitDialog(
            categoryName = cat.name,
            currentPaise = cat.monthlyBudgetInPaise ?: 0L,
            onConfirm = { paise -> viewModel.setCategoryBudget(cat, paise); editing = null },
            onClear = { viewModel.setCategoryBudget(cat, 0L); editing = null },
            onDismiss = { editing = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeaderIcon(onClick = onBack)
                Text(
                    text = "Budgets",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = state.monthLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { OverallCard(state, Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "BY CATEGORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Manage categories",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = KaasuColors.forest,
                    modifier = Modifier.clickable(onClick = onEditBudgets)
                )
            }
        }

        if (state.categoryBudgets.isEmpty() && !state.isLoading) {
            item {
                Text(
                    text = "No category limits yet. Tap a category below to set a monthly limit.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        } else {
            items(state.categoryBudgets, key = { it.category.id }) { cb ->
                CategoryBudgetCard(cb, Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) { editing = cb.category }
            }
        }

        if (state.unbudgetedCategories.isNotEmpty()) {
            item {
                Text(
                    text = "ADD A LIMIT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 8.dp)
                )
            }
            items(state.unbudgetedCategories, key = { "u-${it.id}" }) { cat ->
                AddLimitRow(cat.name, Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { editing = cat }
            }
        }
    }
}

@Composable
private fun AddLimitRow(name: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text("Set limit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KaasuColors.forest)
    }
}

@Composable
private fun SetLimitDialog(
    categoryName: String,
    currentPaise: Long,
    onConfirm: (Long) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(if (currentPaise > 0) (currentPaise / 100).toString() else "") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                val paise = text.toLongOrNull()?.times(100) ?: 0L
                onConfirm(paise)
            }) { Text("Save", color = KaasuColors.forest, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            if (currentPaise > 0) androidx.compose.material3.TextButton(onClick = onClear) { Text("Remove", color = KaasuColors.expense) }
            else androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        title = { Text("$categoryName limit", fontWeight = FontWeight.ExtraBold) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit) },
                label = { Text("Monthly limit (₹)") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
        }
    )
}

@Composable
private fun OverallCard(state: BudgetsUiState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
            ProgressRing(progress = state.overallProgress, over = state.overallSpentInPaise > state.overallBudgetInPaise && state.overallBudgetInPaise > 0)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${state.overallPct}%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "USED",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "OVERALL",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = state.overallSpentInPaise.formatRupees(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (state.overallBudgetInPaise > 0) {
                    Text(
                        text = " / ${state.overallBudgetInPaise.formatRupees()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
            if (state.overallBudgetInPaise > 0) {
                val onPaceOver = state.projectedInPaise > state.overallBudgetInPaise
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background((if (onPaceOver) KaasuColors.expense else KaasuColors.income).copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${if (onPaceOver) "⚠ " else ""}On pace for ${state.projectedInPaise.formatRupees()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (onPaceOver) KaasuColors.expense else KaasuColors.income
                    )
                }
            } else {
                Text(
                    text = "No overall budget set",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProgressRing(progress: Float, over: Boolean) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val arc = if (over) KaasuColors.expense else KaasuColors.forest
    Canvas(modifier = Modifier.size(84.dp)) {
        val stroke = 9.dp.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = track,
            startAngle = 0f, sweepAngle = 360f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = arc,
            startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun CategoryBudgetCard(cb: CategoryBudget, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val color = cb.category.color?.let { runCatching { it.toComposeColor() }.getOrNull() } ?: KaasuColors.forest
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cb.category.name.take(1).uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(Modifier.size(10.dp))
            Text(
                text = cb.category.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = cb.spentInPaise.formatRupees(),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (cb.isOver) KaasuColors.expense else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = " / ${cb.budgetInPaise.formatRupees()}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(cb.progress)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (cb.isOver) KaasuColors.expense else color)
            )
        }

        Text(
            text = if (cb.isOver) "Over by ${cb.overByPaise.formatRupees()} · ${cb.overByPct}%"
            else "${cb.remainingInPaise.formatRupees()} to spare",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (cb.isOver) KaasuColors.expense else KaasuColors.income
        )
    }
}

@Composable
private fun HeaderIcon(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp)
        )
    }
}
