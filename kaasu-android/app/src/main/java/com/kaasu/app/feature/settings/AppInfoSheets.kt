package com.kaasu.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaasu.app.ui.theme.KaasuColors
import com.kaasu.app.BuildConfig

/** Per-version "What's new" content. Update [highlights] each release. */
object WhatsNew {
    const val VERSION_CODE = BuildConfig.VERSION_CODE
    val title = "What's new in ${BuildConfig.VERSION_NAME}"
    val highlights = listOf(
        "Real app lock with PIN + optional fingerprint.",
        "Back up & restore all your data as a file.",
        "Budgets, Subscriptions, and a 'Needs a tag' review queue.",
        "Smarter capture — fewer promos, better merchant names.",
    )
}

@Composable
fun WhatsNewDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("Got it", color = KaasuColors.forest, fontWeight = FontWeight.Bold) } },
        icon = { Icon(Icons.Default.NewReleases, contentDescription = null, tint = KaasuColors.forest) },
        title = { Text(WhatsNew.title, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WhatsNew.highlights.forEach { line ->
                    Row {
                        Text("•  ", color = KaasuColors.forest, fontWeight = FontWeight.Bold)
                        Text(line, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    )
}

@Composable
fun AboutDialog(onWhatsNew: () -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("Close", color = KaasuColors.forest, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onWhatsNew) { Text("What's new", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        icon = {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(KaasuColors.forest),
                contentAlignment = Alignment.Center
            ) { Text("K", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = KaasuColors.onForest) }
        },
        title = { Text("Kaasu", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                Text("காசு · Tamil for money", fontSize = 13.sp, color = KaasuColors.forest)
                Text("Version ${BuildConfig.VERSION_NAME}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Text(
                    "A privacy-first spending tracker for Indian users. Reads UPI and bank notifications locally — no cloud, no accounts, no tracking.",
                    fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(10.dp))
                Text("Made in Chennai", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KaasuColors.forest)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportSheet(
    onBug: () -> Unit,
    onFeature: () -> Unit,
    onHelp: () -> Unit,
    onCopyDiagnostics: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Support", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text("Get help, report issues, or request a feature.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            SupportRow(Icons.Default.BugReport, "Report a bug", "Email the developer with a ready-made template.", onBug)
            SupportRow(Icons.Default.Lightbulb, "Request a feature", "Email a feature idea — just fill it in.", onFeature)
            SupportRow(Icons.AutoMirrored.Filled.HelpOutline, "Help & FAQ", "Common questions about capture, editing and categories.", onHelp)
            SupportRow(Icons.Default.ContentCopy, "Copy diagnostics ID", "App + device info to include in a report.", onCopyDiagnostics)
            Spacer(Modifier.height(8.dp))
            Text("Usually replies within 24–48 hours.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun SupportRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = KaasuColors.forest, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Diagnostics ID string for support emails (no personal data). */
fun diagnosticsId(): String =
    "Kaasu ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} · Android ${android.os.Build.VERSION.RELEASE}"
