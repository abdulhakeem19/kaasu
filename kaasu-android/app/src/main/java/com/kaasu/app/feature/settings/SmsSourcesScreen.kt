package com.kaasu.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaasu.app.ui.theme.KaasuColors
import com.kaasu.app.core.database.entity.SmsSenderEntity
import kotlin.math.absoluteValue

/**
 * Settings screen for SMS senders auto-discovered by SmsFilter (unlike BankSourcesScreen's
 * curated apps, senders here are populated purely by what has actually been seen — there's no
 * "add a sender" flow to mirror BankSourcesScreen's "+ Add bank").
 */
@Composable
fun SmsSourcesScreen(
    onBack: () -> Unit,
    viewModel: SmsSourcesViewModel = hiltViewModel()
) {
    val senders by viewModel.smsSenders.collectAsStateWithLifecycle()

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
                Text("SMS sources", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            }
        }

        item {
            Text(
                text = "Senders Kaasu has seen a bank/UPI SMS from. Turn any off to stop capturing from it.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        item {
            Column(
                modifier = Modifier.padding(16.dp).clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                if (senders.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No SMS senders seen yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    senders.forEachIndexed { index, sender ->
                        SmsSenderRow(
                            sender = sender,
                            onToggle = { enabled -> viewModel.toggleSender(sender, enabled) },
                            isLast = index == senders.lastIndex
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmsSenderRow(
    sender: SmsSenderEntity,
    onToggle: (Boolean) -> Unit,
    isLast: Boolean
) {
    val initial = sender.senderId.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val chipColor = senderToColor(sender.senderId)
    val statusText = if (sender.isEnabled) {
        sender.lastSeenAt?.let { "Active · last SMS ${relativeSmsTime(it)}" } ?: "Active · no SMS yet"
    } else "Off"

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(chipColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(initial, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = chipColor))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    sender.displayLabel ?: sender.senderId,
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace)
                )
                Text(statusText, style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant), maxLines = 1)
            }
            Switch(
                checked = sender.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = KaasuColors.onForest, checkedTrackColor = KaasuColors.forest, uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant, uncheckedTrackColor = MaterialTheme.colorScheme.outline)
            )
        }
        if (!isLast) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 14.dp))
    }
}

private fun senderToColor(senderId: String): Color {
    val colors = listOf(Color(0xFF1A73E8), Color(0xFF5F259F), Color(0xFF1F8458), Color(0xFFA36B00))
    return colors[(senderId.hashCode().absoluteValue) % colors.size]
}

private fun relativeSmsTime(epochMillis: Long): String {
    val diff = System.currentTimeMillis() - epochMillis
    val mins = diff / 60_000; val hours = mins / 60; val days = hours / 24
    return when {
        mins < 1 -> "just now"; mins < 60 -> "${mins}m ago"; hours < 24 -> "${hours}h ago"
        days < 30 -> "${days}d ago"; else -> "${days / 30}mo ago"
    }
}
