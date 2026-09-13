package com.kaasu.app.feature.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaasu.app.ui.theme.KaasuColors
import com.kaasu.app.core.util.formatRupees
import com.kaasu.app.domain.usecase.subscription.Subscription

@Composable
fun SubscriptionsScreen(
    onBack: () -> Unit,
    onSubscriptionClick: (String) -> Unit = {},
    viewModel: SubscriptionsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showAdd by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showAdd) {
        AddSubscriptionDialog(
            onAdd = { merchant, amount, billingDay ->
                viewModel.addManual(merchant, amount, billingDay) { ok ->
                    if (ok) showAdd = false
                    else android.widget.Toast.makeText(context, "Enter a name, amount and renewal day", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showAdd = false }
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
                HeaderIcon(onBack)
                Text(
                    text = "Subscriptions",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .clickable { showAdd = true }
                        .background(KaasuColors.forest.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("+ Add", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KaasuColors.forest)
                }
            }
        }

        item { SummaryCard(state, Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }

        if (!state.isLoading &&
            state.renewsThisWeek.isEmpty() && state.unused.isEmpty() && state.otherActive.isEmpty()
        ) {
            item {
                Text(
                    text = "No subscriptions detected yet. Kaasu spots them after a couple of monthly charges from the same merchant — or mark any transaction as recurring.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
                )
            }
        }

        section("RENEWS THIS WEEK", state.renewsThisWeek, onSubscriptionClick)
        section("NOT USED IN 60 DAYS", state.unused, onSubscriptionClick)
        section("OTHER ACTIVE", state.otherActive, onSubscriptionClick)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    items: List<Subscription>,
    onClick: (String) -> Unit
) {
    if (items.isEmpty()) return
    item {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 8.dp)
        )
    }
    items(items.size, key = { "$title-${items[it].merchantName}" }) { i ->
        val sub = items[i]
        SubscriptionCard(sub, Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { onClick(sub.merchantName) }
    }
}

@Composable
private fun SummaryCard(state: SubscriptionsUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(KaasuColors.forest)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "RECURRING · PER MONTH",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = KaasuColors.onForest.copy(alpha = 0.75f)
        )
        Text(
            text = state.monthlyTotalInPaise.formatRupees(),
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp,
            color = KaasuColors.onForest
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill("${state.activeCount} active")
            if (state.unusedCount > 0) {
                Pill("${state.unusedCount} unused · ${state.unusedTotalInPaise.formatRupees()}")
            }
        }
    }
}

@Composable
private fun Pill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(KaasuColors.onForest.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KaasuColors.onForest)
    }
}

@Composable
private fun SubscriptionCard(sub: Subscription, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(KaasuColors.forest.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = sub.merchantName.take(1).uppercase(),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = KaasuColors.forest
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = sub.merchantName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = subtitle(sub),
                fontSize = 12.sp,
                color = if (sub.isLikelyUnused) KaasuColors.expense else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = sub.typicalAmountInPaise.formatRupees(),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = renewalLabel(sub),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (sub.isLikelyUnused) KaasuColors.expense
                else if (sub.daysUntilRenewal in 0..3) KaasuColors.expense
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun subtitle(sub: Subscription): String {
    val cadence = if (sub.cadenceDays in 26..35) "monthly" else "every ${sub.cadenceDays}d"
    return if (sub.isLikelyUnused) "No charge in ${sub.daysSinceLastCharge} days"
    else if (sub.isManuallyFlagged) "Marked recurring · $cadence"
    else "$cadence · ${sub.chargeCount} charges"
}

private fun renewalLabel(sub: Subscription): String {
    if (sub.isLikelyUnused) return "review"
    val d = sub.daysUntilRenewal
    return when {
        d < 0 -> "overdue"
        d == 0 -> "today"
        d == 1 -> "tomorrow"
        else -> "in $d days"
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

@Composable
private fun AddSubscriptionDialog(onAdd: (String, String, Int) -> Unit, onDismiss: () -> Unit) {
    var merchant by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var renewDay by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onAdd(merchant, amount, renewDay.toIntOrNull() ?: 0) }) {
                Text("Add", color = KaasuColors.forest, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        title = { Text("Add subscription", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Name (e.g. Netflix)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monthly amount (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = renewDay,
                    onValueChange = { renewDay = it.filter(Char::isDigit).take(2) },
                    label = { Text("Renews on day of month (1–28)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    )
}
