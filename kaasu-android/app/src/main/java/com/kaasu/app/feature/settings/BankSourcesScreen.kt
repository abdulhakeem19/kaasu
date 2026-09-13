package com.kaasu.app.feature.settings

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaasu.app.ui.theme.KaasuColors
import com.kaasu.app.accessibility.AccessibilityServiceStatus
import com.kaasu.app.core.database.entity.AppSourceEntity
import com.kaasu.app.notification.filter.SourceApps
import kotlin.math.absoluteValue

@Composable
fun BankSourcesScreen(
    onBack: () -> Unit,
    viewModel: BankSourcesViewModel = hiltViewModel()
) {
    val sources by viewModel.appSources.collectAsStateWithLifecycle()
    var selectedSource by remember { mutableStateOf<AppSourceEntity?>(null) }
    var showAddBank by remember { mutableStateOf(false) }

    selectedSource?.let { src ->
        BankSourceDetailsSheet(
            source = src,
            onToggle = { enabled -> viewModel.toggleAppSource(src, enabled); selectedSource = src.copy(isEnabled = enabled) },
            onDismiss = { selectedSource = null },
            hasScreenReading = src.packageName in viewModel.accessibilityScrapedPackages
        )
    }
    if (showAddBank) {
        AddBankSheet(
            existingPackages = sources.map { it.packageName }.toSet(),
            onAdd = { pkg, name -> viewModel.addAppSource(pkg, name); showAddBank = false },
            onDismiss = { showAddBank = false }
        )
    }

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
                Text("Bank sources", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(50.dp)).clickable { showAddBank = true }
                        .background(KaasuColors.forest.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("+ Add bank", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KaasuColors.forest)
                }
            }
        }

        item {
            Text(
                text = "Kaasu reads payment alerts only from the apps enabled here.",
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
                if (sources.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No sources yet. Tap + Add bank.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    sources.forEachIndexed { index, source ->
                        SmsBankRow(
                            source = source,
                            onToggle = { enabled -> viewModel.toggleAppSource(source, enabled) },
                            onClick = { selectedSource = source },
                            isLast = index == sources.lastIndex
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmsBankRow(
    source: AppSourceEntity,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    isLast: Boolean
) {
    val initial = source.appName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val chipColor = packageToColor(source.packageName)
    val statusText = if (source.isEnabled) {
        source.lastSeenAt?.let { "Active · last alert ${relativeTime(it)}" } ?: "Active · no alerts yet"
    } else "Off"

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 11.dp),
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
                Text(source.appName, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface))
                Text(statusText, style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant), maxLines = 1)
            }
            Switch(
                checked = source.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = KaasuColors.onForest, checkedTrackColor = KaasuColors.forest, uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant, uncheckedTrackColor = MaterialTheme.colorScheme.outline)
            )
        }
        if (!isLast) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 14.dp))
    }
}

private fun packageToColor(pkg: String): Color {
    val colors = listOf(Color(0xFF1A73E8), Color(0xFF5F259F), Color(0xFF1F8458), Color(0xFFA36B00))
    return colors[(pkg.hashCode().absoluteValue) % colors.size]
}

private fun relativeTime(epochMillis: Long): String {
    val diff = System.currentTimeMillis() - epochMillis
    val mins = diff / 60_000; val hours = mins / 60; val days = hours / 24
    return when {
        mins < 1 -> "just now"; mins < 60 -> "${mins}m ago"; hours < 24 -> "${hours}h ago"
        days < 30 -> "${days}d ago"; else -> "${days / 30}mo ago"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankSourceDetailsSheet(
    source: AppSourceEntity,
    onToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    hasScreenReading: Boolean
) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(source.appName, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface))
            DetailLine("Type", if (source.isKnownFinanceApp) "Payment / bank app" else "Other")
            DetailLine("Package", source.packageName, mono = true)
            DetailLine("Status", if (source.isEnabled) "Active" else "Off")
            DetailLine("Last alert", source.lastSeenAt?.let { relativeTime(it) } ?: "None yet")
            if (hasScreenReading) {
                // Health signal for the accessibility screen-scrape channel (fails silently
                // otherwise) — see AppSourceDao.updateLastAccessibilityScrapeAttempt/Success.
                val screenReadingStatus = when {
                    source.lastAccessibilityScrapeSuccessAt != null ->
                        "Last successful read ${relativeTime(source.lastAccessibilityScrapeSuccessAt)}"
                    source.lastAccessibilityScrapeAttemptAt != null ->
                        "Attempted ${relativeTime(source.lastAccessibilityScrapeAttemptAt)}, nothing new found"
                    else -> "Not attempted yet"
                }
                DetailLine("Screen reading", screenReadingStatus)

                val accessibilityEnabled = remember { AccessibilityServiceStatus.isEnabled(context) }
                Row(
                    modifier = Modifier.fillMaxWidth().let {
                        if (!accessibilityEnabled) {
                            it.clickable { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                        } else it
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (accessibilityEnabled) {
                            "Screen-reading service is on (system setting)"
                        } else {
                            "Screen-reading service is off — tap to enable in Settings"
                        },
                        modifier = Modifier.weight(1f),
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (accessibilityEnabled) MaterialTheme.colorScheme.onSurfaceVariant else KaasuColors.forest
                        )
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Capture from this app", modifier = Modifier.weight(1f), style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface))
                Switch(
                    checked = source.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = KaasuColors.onForest, checkedTrackColor = KaasuColors.forest, uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant, uncheckedTrackColor = MaterialTheme.colorScheme.outline)
                )
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String, mono: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(96.dp), style = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
        Text(
            value, modifier = Modifier.weight(1f),
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBankSheet(existingPackages: Set<String>, onAdd: (String, String) -> Unit, onDismiss: () -> Unit) {
    val available = SourceApps.PAYMENT_APP_PACKAGES.filter { it !in existingPackages }.sortedBy { SourceApps.displayName(it) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Add a bank or UPI app", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface), modifier = Modifier.padding(vertical = 8.dp))
            if (available.isEmpty()) {
                Text("All known apps are already added.", style = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.padding(vertical = 8.dp))
            }
            available.forEach { pkg ->
                val name = SourceApps.displayName(pkg)
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onAdd(pkg, name) }.padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(packageToColor(pkg).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(name.first().uppercase(), style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = packageToColor(pkg)))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(name, modifier = Modifier.weight(1f), style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface))
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = KaasuColors.forest, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
