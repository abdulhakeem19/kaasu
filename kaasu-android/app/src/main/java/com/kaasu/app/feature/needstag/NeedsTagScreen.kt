package com.kaasu.app.feature.needstag

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaasu.app.domain.money.SpendRules
import com.kaasu.app.ui.theme.KaasuColors
import com.kaasu.app.core.util.formatRupees
import com.kaasu.app.core.util.toComposeColor
import com.kaasu.app.domain.model.Category
import com.kaasu.app.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NeedsTagScreen(
    onBack: () -> Unit,
    viewModel: NeedsTagViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Per-transaction "always tag this merchant" choice
    val alwaysFlags = remember { mutableStateMapOf<Long, Boolean>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeaderIcon(onBack)
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Needs a tag",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (state.isLoading) "" else "${state.items.size} to review",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!state.isLoading && state.items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = KaasuColors.income,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("All caught up", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Every transaction has a category.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            state.items.forEach { item ->
                TagCard(
                    merchant = item.transaction.merchantName ?: "Unknown",
                    subtitle = subtitleFor(item.transaction),
                    amountLabel = amountLabel(item.transaction),
                    isExpense = SpendRules.isOutflow(item.transaction),
                    suggestedCategory = item.suggestedCategoryId?.let { state.categoryMap[it] },
                    categories = state.categories,
                    always = alwaysFlags[item.transaction.id] ?: true,
                    onAlwaysChange = { alwaysFlags[item.transaction.id] = it },
                    onPick = { categoryId ->
                        viewModel.tag(item.transaction.id, categoryId, alwaysFlags[item.transaction.id] ?: true)
                    },
                    onSkip = { viewModel.skip(item.transaction.id) }
                )
            }
            Spacer(Modifier.size(8.dp))
        }

        // Footer actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    .clickable { state.items.forEach { viewModel.skip(it.transaction.id) } }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Skip all", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Box(
                modifier = Modifier
                    .weight(2f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(KaasuColors.forest)
                    .clickable { viewModel.confirmAllSuggestions() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Confirm all suggestions", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = KaasuColors.onForest)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagCard(
    merchant: String,
    subtitle: String,
    amountLabel: String,
    isExpense: Boolean,
    suggestedCategory: Category?,
    categories: List<Category>,
    always: Boolean,
    onAlwaysChange: (Boolean) -> Unit,
    onPick: (Long) -> Unit,
    onSkip: () -> Unit
) {
    // Suggested category first, then a few common others.
    val chips = buildList {
        suggestedCategory?.let { add(it) }
        categories.filter { it.id != suggestedCategory?.id }.take(4).forEach { add(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant),
                contentAlignment = Alignment.Center
            ) {
                Text("?", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(merchant, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                amountLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isExpense) KaasuColors.expense else KaasuColors.income
            )
        }

        if (suggestedCategory != null) {
            Text(
                text = "Looks like ${suggestedCategory.name} — pick to confirm",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            chips.forEach { category ->
                val isSuggested = category.id == suggestedCategory?.id
                CategoryChip(category, highlighted = isSuggested, onClick = { onPick(category.id) })
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = always,
                onCheckedChange = onAlwaysChange,
                colors = CheckboxDefaults.colors(checkedColor = KaasuColors.forest)
            )
            Text(
                text = "Always tag $merchant as this",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Skip",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSkip)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun CategoryChip(category: Category, highlighted: Boolean, onClick: () -> Unit) {
    val dot = category.color?.let { runCatching { it.toComposeColor() }.getOrNull() } ?: KaasuColors.forest
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .then(
                if (highlighted) Modifier.background(KaasuColors.forest)
                else Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(100.dp))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (highlighted) KaasuColors.onForest else dot)
        )
        Spacer(Modifier.size(6.dp))
        Text(
            category.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (highlighted) KaasuColors.onForest else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun subtitleFor(tx: com.kaasu.app.domain.model.Transaction): String {
    val date = SimpleDateFormat("d MMM · h:mm a", Locale.getDefault()).format(Date(tx.transactionTime))
    return date
}

private fun amountLabel(tx: com.kaasu.app.domain.model.Transaction): String {
    val sign = if (SpendRules.isOutflow(tx)) "−₹" else "+₹"
    return "$sign${tx.amountInPaise.formatRupees().removePrefix("₹")}"
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
