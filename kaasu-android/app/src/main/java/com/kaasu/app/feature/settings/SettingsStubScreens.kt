package com.kaasu.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaasu.app.ui.theme.KaasuColors
import com.kaasu.app.BuildConfig

@Composable
fun HelpScreen(onBack: () -> Unit) {
    StubSettingsScreen(title = "Help & FAQ", onBack = onBack) {
        StubSection("GETTING STARTED") {
            FaqRow("How does Kaasu read my transactions?", "Kaasu watches notifications from your UPI and bank apps (with your permission) and parses payment alerts on-device. It never reads your SMS inbox or uploads anything.")
            FaqRow("Is my data safe?", "Everything is stored locally on your phone. There are no accounts, no cloud sync, and no analytics that capture your transactions.")
            FaqRow("Which apps are supported?", "Major UPI and bank apps — GPay, PhonePe, Paytm, BHIM, CRED and bank apps — plus bank SMS via your messaging app. You can manage them under Bank sources.", isLast = true)
        }
        Spacer(Modifier.height(12.dp))
        StubSection("TRANSACTIONS") {
            FaqRow("Why is a transaction missing?", "It may have come from an app that isn't in your Bank sources, or the notification didn't look like a real payment. You can always add it manually with the + button.")
            FaqRow("How do I edit a transaction?", "Open the transaction and tap the menu (⋮) → Edit. You can change the payee, category, account, date and note.")
            FaqRow("How do I add one manually?", "Tap the + button in the bottom bar, enter the amount on the keypad, pick a category and account, then Save.", isLast = true)
        }
        Spacer(Modifier.height(12.dp))
        StubSection("CATEGORIES") {
            FaqRow("How do custom categories work?", "Manage categories from Settings → Categories. Each can have a colour and a monthly budget that feeds the Budgets screen.")
            FaqRow("What are auto-tags?", "Merchant rules that automatically assign a category when a known merchant is seen. Confirming a suggestion in 'Needs a tag' with 'Always tag' creates one.", isLast = true)
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    StubSettingsScreen(title = "About Kaasu", onBack = onBack) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(KaasuColors.forest),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "K",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = KaasuColors.onForest
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Kaasu",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "காசு · Tamil for money",
                    fontSize = 13.sp,
                    color = KaasuColors.forest
                )
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "A privacy-first spending tracker for Indian users. Reads UPI and bank notifications locally — no cloud, no accounts.",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Made in Chennai",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = KaasuColors.forest
                )
            }
        }
    }
}

@Composable
fun MerchantRulesScreen(
    onBack: () -> Unit,
    viewModel: MerchantRulesViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    StubSettingsScreen(title = "Merchant Rules", onBack = onBack) {
        Text(
            text = if (rules.isEmpty()) "No auto-tags yet" else "${rules.size} auto-tags active",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        if (rules.isEmpty()) {
            Text(
                text = "When you confirm a category in 'Needs a tag' with \"Always tag\", a rule is created here so the same merchant is categorised automatically next time.",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                rules.forEachIndexed { index, rule ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rule.matchText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text("→ ${rule.categoryName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!rule.isSystem) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete rule",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp).clickable { viewModel.deleteRule(rule.id) }
                            )
                        }
                    }
                    if (index < rules.lastIndex) {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 14.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    }
                }
            }
        }
    }
}

@Composable
fun LegalScreen(doc: String, onBack: () -> Unit) {
    val isPrivacy = doc == "privacy"
    val title = if (isPrivacy) "Privacy policy" else "Terms of service"
    val body = if (isPrivacy) PRIVACY_TEXT else TERMS_TEXT
    StubSettingsScreen(title = title, onBack = onBack) {
        Text(
            text = body,
            fontSize = 13.sp,
            lineHeight = 21.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private val PRIVACY_TEXT = """
Kaasu is privacy-first. All of your data — transactions, categories, accounts and rules — is stored locally on this device only.

• No accounts, no sign-in, no cloud sync.
• Kaasu never uploads your transactions or notification text to any server.
• Notification access is used solely to read payment alerts from the apps you enable under Bank sources, and parsing happens on-device.
• No third-party analytics capture your financial data.
• You can export your data, or delete all of it, at any time from Settings.

Because everything stays on your device, keeping your phone secure (and enabling App lock) is the best way to protect your data.
""".trimIndent()

private val TERMS_TEXT = """
Kaasu is provided as-is, to help you track your spending.

• Kaasu reads payment notifications to record transactions automatically. Parsing is best-effort — always verify important figures against your bank.
• You are responsible for the accuracy of manually added or edited transactions.
• Kaasu does not move money, make payments, or connect to your bank accounts.
• Your data lives on your device; you are responsible for backups before clearing data or changing devices.

By using Kaasu you accept that it is a personal finance aid and not a substitute for your bank's official statements.
""".trimIndent()

// ── Shared internal components ────────────────────────────────────────────────

@Composable
private fun StubSettingsScreen(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            content()
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun StubSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 6.dp, top = 4.dp)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        content()
    }
}

@Composable
private fun FaqRow(question: String, answer: String, isLast: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = question,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        if (expanded) {
            Text(
                text = answer,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 13.dp)
            )
        }
        if (!isLast) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(horizontal = 14.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}

