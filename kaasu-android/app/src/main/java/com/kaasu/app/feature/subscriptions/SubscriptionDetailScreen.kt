package com.kaasu.app.feature.subscriptions

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalConfiguration
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SubscriptionDetailScreen(
    onBack: () -> Unit,
    viewModel: SubscriptionDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sub = state.subscription
    val dateFmt = SimpleDateFormat("d MMM yyyy", LocalConfiguration.current.locales[0])

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text(sub?.merchantName ?: "Subscription", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            }
        }

        if (sub == null) {
            if (!state.isLoading) item {
                Text("This is no longer detected as a subscription.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp))
            }
            return@LazyColumn
        }

        item { HeroCard(sub, Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }

        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                StatLine("Per charge", sub.typicalAmountInPaise.formatRupees())
                StatLine("Cadence", if (sub.cadenceDays in 26..35) "Monthly" else "Every ${sub.cadenceDays} days")
                StatLine("Charges so far", "${sub.chargeCount}")
                StatLine("Total spent", sub.totalSpentInPaise.formatRupees())
                StatLine("First charged", dateFmt.format(Date(sub.firstChargedAt)))
                StatLine("Last charged", dateFmt.format(Date(sub.lastChargedAt)))
                StatLine(
                    "Next due",
                    if (sub.isLikelyUnused) "Unused ${sub.daysSinceLastCharge}d — review" else dateFmt.format(Date(sub.estimatedNextChargeAt))
                )
            }
        }

        item {
            Text("CHARGES", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 6.dp))
        }
        items(state.charges, key = { it.id }) { tx ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(dateFmt.format(Date(tx.transactionTime)), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(tx.amountInPaise.formatRupees(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun HeroCard(sub: Subscription, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(KaasuColors.forest).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("PER MONTH", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = KaasuColors.onForest.copy(alpha = 0.75f))
        val perMonth = sub.typicalAmountInPaise * 30 / sub.cadenceDays.coerceAtLeast(1)
        Text(perMonth.formatRupees(), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = KaasuColors.onForest)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(if (sub.isLikelyUnused) "Unused" else "Active")
            if (sub.isManuallyFlagged) Pill("Marked recurring")
        }
    }
}

@Composable
private fun Pill(text: String) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(KaasuColors.onForest.copy(alpha = 0.15f)).padding(horizontal = 12.dp, vertical = 6.dp)
    ) { Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KaasuColors.onForest) }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
