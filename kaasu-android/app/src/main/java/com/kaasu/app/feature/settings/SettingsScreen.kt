package com.kaasu.app.feature.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import com.kaasu.app.ui.theme.KaasuColors
import com.kaasu.app.BuildConfig
import com.kaasu.app.accessibility.AccessibilityServiceStatus
import com.kaasu.app.core.database.entity.AppSourceEntity
import com.kaasu.app.core.datastore.SettingsDataStore
import com.kaasu.app.notification.filter.SourceApps
import com.kaasu.app.sms.worker.SmsBackfillWorker
import com.kaasu.app.core.util.parseToPaise
import com.kaasu.app.core.util.toAmountDisplay
import kotlin.math.absoluteValue

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    onNavigateToCategories: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToBankSources: () -> Unit = {},
    onNavigateToSmsSources: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToNeedsTag: () -> Unit = {},
    onNavigateToDuplicates: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit,
    onNavigateToAppLock: () -> Unit = {},
    onNavigateToMerchantRules: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToLegal: (String) -> Unit = {},
    onNavigateToImportStatement: (uri: String, mimeType: String) -> Unit = { _, _ -> },
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val budget by viewModel.monthlyBudgetInPaise.collectAsStateWithLifecycle()
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val displayName by viewModel.displayName.collectAsStateWithLifecycle()
    val categoryCount by viewModel.categoryCount.collectAsStateWithLifecycle()
    val ruleCount by viewModel.ruleCount.collectAsStateWithLifecycle()
    val accountCount by viewModel.accountCount.collectAsStateWithLifecycle()
    val appSources by viewModel.appSources.collectAsStateWithLifecycle()
    val smsSenders by viewModel.smsSenders.collectAsStateWithLifecycle()
    val smsBackfillWorkInfos by viewModel.smsBackfillWorkInfos.collectAsStateWithLifecycle()
    val isRescanning by viewModel.isRescanning.collectAsStateWithLifecycle()
    val screenReadingHealth by viewModel.screenReadingHealth.collectAsStateWithLifecycle()
    val hideAmountsOnLock by viewModel.hideAmountsOnLock.collectAsStateWithLifecycle()
    val budgetAlertsOn by viewModel.budgetAlertsEnabled.collectAsStateWithLifecycle()
    val subscriptionRenewalsOn by viewModel.subscriptionRenewalsEnabled.collectAsStateWithLifecycle()
    val dailyNudgeOn by viewModel.dailyNudgeEnabled.collectAsStateWithLifecycle()
    val weeklySummaryOn by viewModel.weeklySummaryEnabled.collectAsStateWithLifecycle()
    val appLockEnabled by viewModel.appLockEnabled.collectAsStateWithLifecycle()
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()

    var isNotificationGranted by remember { mutableStateOf(false) }
    var isSmsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showWhatsNew by remember { mutableStateOf(false) }
    var showSupport by remember { mutableStateOf(false) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.RECEIVE_SMS] == true &&
            grants[Manifest.permission.READ_SMS] == true
        isSmsGranted = granted
        if (granted) SmsBackfillWorker.enqueue(context)
    }

    // Recheck notification/SMS access every time this screen becomes visible
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNotificationGranted = NotificationManagerCompat
                    .getEnabledListenerPackages(context)
                    .contains(context.packageName)
                isSmsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                    PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all transactions?") },
            text = { Text("This will permanently delete every transaction on this device. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    onNavigateToPrivacy()
                }) {
                    Text("Clear", color = KaasuColors.expense)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showBudgetDialog) {
        BudgetDialog(
            currentBudgetInPaise = budget,
            onConfirm = { paise ->
                viewModel.setBudget(paise)
                showBudgetDialog = false
            },
            onClear = {
                viewModel.clearBudget()
                showBudgetDialog = false
            },
            onDismiss = { showBudgetDialog = false }
        )
    }

    // Restore: pick a backup .json file
    val restorePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.restoreData(it) } }

    // Import bank statement: pick a CSV or PDF file, then hand off to ImportStatementScreen
    val importStatementPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val mimeType = context.contentResolver.getType(it) ?: ""
            onNavigateToImportStatement(it.toString(), mimeType)
        }
    }

    // Share an exported file (backup .json or CSV)
    LaunchedEffect(Unit) {
        viewModel.shareFile.collect { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share or save"))
        }
    }
    // Snackbar/toast messages (restore result, errors)
    LaunchedEffect(Unit) {
        viewModel.message.collect { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    if (showAbout) {
        AboutDialog(
            onWhatsNew = { showAbout = false; showWhatsNew = true },
            onClose = { showAbout = false }
        )
    }
    if (showWhatsNew) {
        WhatsNewDialog(onClose = { showWhatsNew = false })
    }
    if (showSupport) {
        SupportSheet(
            onBug = { showSupport = false; context.sendSupportEmail("Kaasu bug report") },
            onFeature = { showSupport = false; context.sendSupportEmail("Kaasu feature request") },
            onHelp = { showSupport = false; onNavigateToHelp() },
            onCopyDiagnostics = {
                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("Kaasu diagnostics", diagnosticsId()))
                android.widget.Toast.makeText(context, "Diagnostics ID copied", android.widget.Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showSupport = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {

        // ── Header row ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Settings",
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.8).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        // ── Profile card ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                .clickable(onClick = onNavigateToProfile)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar chip — initial from the user's name
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.trim().firstOrNull()?.uppercase() ?: "K",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName.ifBlank { "Set up your profile" },
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = buildString {
                        append(if (accountCount == 0) "No accounts linked" else "$accountCount account${if (accountCount == 1) "" else "s"} linked")
                    },
                    style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        // ── Privacy hero card — intentional KaasuColors.forest bg ───────────────────
        Column(
            modifier = Modifier
                .padding(start = 14.dp, end = 14.dp, top = 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(KaasuColors.forest)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Shield chip
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = KaasuColors.onForest,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Your data lives here",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = KaasuColors.onForest
                        )
                    )
                    Text(
                        text = "Local-only. No accounts. No cloud.",
                        style = TextStyle(
                            fontSize = 11.5.sp,
                            color = KaasuColors.onForest.copy(alpha = 0.8f)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrivacyChip(label = "Encrypted backup", modifier = Modifier.weight(1f))
                PrivacyChip(label = "Export CSV", modifier = Modifier.weight(1f))
            }
        }

        // ── BUDGET ────────────────────────────────────────────────────────────
        SettingsSection(title = "BUDGET")

        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBudgetDialog = true }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Monthly budget",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (budget > 0) budget.toAmountDisplay() else "Not set",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(KaasuColors.forest)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Edit",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = KaasuColors.onForest
                        )
                    )
                }
            }
        }

        // ── BANK SOURCES ────────────────────────────────────────────────────────
        SettingsSection(title = "BANK SOURCES")
        SettingsCard {
            SettingsRow(
                label = "Bank & UPI apps",
                subtitle = if (appSources.isEmpty()) "Tap to add"
                    else "${appSources.size} apps · ${appSources.count { it.isEnabled }} active",
                onClick = onNavigateToBankSources,
                isLast = false
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 14.dp))

            // The accounts screen, its view model and its nav route all existed, but nothing ever
            // called onNavigateToAccounts — the whole screen was unreachable from the UI.
            SettingsRow(
                label = "Accounts & cards",
                subtitle = "Bank accounts and cards detected from your transactions",
                onClick = onNavigateToAccounts,
                isLast = false
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 14.dp))

            // Hoisted from three taps deep inside Bank Sources. This signal read "never attempted"
            // for the whole life of the screen-reading channel and nothing surfaced it — a capture
            // channel that silently does nothing is exactly what deserves to be visible.
            val screenReadingEnabled = AccessibilityServiceStatus.isEnabled(context)
            SettingsRow(
                label = "Screen reading",
                subtitle = when {
                    !screenReadingEnabled -> "Off — optional extra capture from GPay/PhonePe screens"
                    screenReadingHealth.hasEverCaptured ->
                        "On · last caught a payment ${relativeTimeShort(screenReadingHealth.lastSuccessAt!!)}"
                    screenReadingHealth.hasEverAttempted ->
                        "On · read a screen ${relativeTimeShort(screenReadingHealth.lastAttemptAt!!)}, nothing new"
                    else -> "On · hasn't captured anything yet"
                },
                onClick = if (screenReadingEnabled) null else {
                    { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                },
                isLast = true
            )
        }

        // ── SMS CAPTURE ───────────────────────────────────────────────────────
        SettingsSection(title = "SMS CAPTURE")
        SettingsCard {
            // SMS access status row (mirrors the notification-access row below)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        smsPermissionLauncher.launch(
                            arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                        )
                    }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSmsGranted) KaasuColors.income else KaasuColors.expense)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SMS access",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                    Text(
                        text = if (isSmsGranted) "Granted · optional, additive to notifications"
                               else "Not granted · tap to enable (optional)",
                        style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(start = 14.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Manage",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KaasuColors.forest)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 14.dp))

            SettingsRow(
                label = "SMS sources",
                subtitle = if (smsSenders.isEmpty()) "No senders seen yet"
                    else "${smsSenders.size} senders · ${smsSenders.count { it.isEnabled }} active",
                onClick = onNavigateToSmsSources,
                isLast = false
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 14.dp))

            val activeBackfill = smsBackfillWorkInfos.firstOrNull { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
            val lastFinishedBackfill = smsBackfillWorkInfos.firstOrNull { it.state == WorkInfo.State.SUCCEEDED }
            val rescanSubtitle = when {
                activeBackfill != null -> {
                    val processed = activeBackfill.progress.getInt("processed", -1)
                    val total = activeBackfill.progress.getInt("total", -1)
                    if (processed >= 0 && total > 0) "Scanning… $processed / $total" else "Scanning…"
                }
                lastFinishedBackfill != null -> {
                    val inserted = lastFinishedBackfill.outputData.getInt("inserted", 0)
                    "Last scan added $inserted transaction${if (inserted == 1) "" else "s"}"
                }
                else -> "Scan your SMS inbox for past bank/UPI messages"
            }
            SettingsRow(
                label = "Re-scan SMS inbox",
                subtitle = rescanSubtitle,
                onClick = if (isSmsGranted && activeBackfill == null) viewModel::rescanSms else null,
                isLast = false
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 14.dp))

            SettingsRow(
                label = "Re-scan saved transactions",
                subtitle = if (isRescanning) "Re-scanning…"
                    else "Fill in missing merchant names, categories and notes on transactions you already have",
                onClick = if (isRescanning) null else viewModel::rescanSavedTransactions,
                isLast = false
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 14.dp))

            SettingsRow(
                label = "Exclude non-transactions",
                subtitle = "Hide old promotional alerts and \"will be debited\" reminders from your totals",
                onClick = viewModel::ignoreNonTransactions,
                isLast = true
            )
        }

        // ── SECURITY ──────────────────────────────────────────────────────────
        SettingsSection(title = "SECURITY")

        SettingsCard {
            IconSettingsRow(
                icon = Icons.Default.Lock,
                label = "App lock",
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = when {
                                !appLockEnabled -> "Off"
                                biometricEnabled -> "PIN + Fingerprint"
                                else -> "PIN"
                            },
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                onClick = onNavigateToAppLock,
                isLast = false
            )
            IconSettingsRow(
                icon = Icons.Default.VisibilityOff,
                label = "Hide amounts on lock",
                trailing = {
                    Switch(
                        checked = hideAmountsOnLock,
                        onCheckedChange = { viewModel.setHideAmountsOnLock(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = KaasuColors.onForest,
                            checkedTrackColor = KaasuColors.forest,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline
                        )
                    )
                },
                onClick = null,
                isLast = false
            )
            IconSettingsRow(
                icon = Icons.Default.Edit,
                label = "Change PIN",
                trailing = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = onNavigateToAppLock,
                isLast = true
            )
        }

        // ── APPEARANCE ────────────────────────────────────────────────────────
        SettingsSection(title = "APPEARANCE")

        SettingsCard {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    text = "Theme",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                ThemePicker(
                    selected = appTheme,
                    onSelect = { viewModel.setTheme(it) }
                )
            }
        }

        // ── NOTIFICATIONS ─────────────────────────────────────────────────────
        SettingsSection(title = "NOTIFICATIONS")

        SettingsCard {
            // Notification access row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        )
                    }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isNotificationGranted) KaasuColors.income else KaasuColors.expense
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Notification access",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                    Text(
                        text = if (isNotificationGranted) "Granted · ${appSources.count { it.isEnabled }} sources"
                               else "Not granted · tap to enable",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(start = 14.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Manage",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = KaasuColors.forest
                        )
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 14.dp)
            )

            // Budget alerts row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Budget alerts",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "At 80%, 100%",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                Switch(
                    checked = budgetAlertsOn,
                    onCheckedChange = { viewModel.setBudgetAlerts(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = KaasuColors.onForest,
                        checkedTrackColor = KaasuColors.forest,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 14.dp)
            )

            // Daily nudge row
            ToggleRow(
                label = "Daily nudge",
                subtitle = "9:00 PM",
                checked = dailyNudgeOn,
                onCheckedChange = { viewModel.setDailyNudge(it) }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 14.dp)
            )

            // Weekly summary row
            ToggleRow(
                label = "Weekly summary",
                subtitle = "Sun · 8 AM",
                checked = weeklySummaryOn,
                onCheckedChange = { viewModel.setWeeklySummary(it) }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 14.dp)
            )

            // Subscription renewals
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subscription renewals",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = subscriptionRenewalsOn,
                    onCheckedChange = { viewModel.setSubscriptionRenewals(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = KaasuColors.onForest,
                        checkedTrackColor = KaasuColors.forest,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }

        // ── CATEGORIES & RULES ────────────────────────────────────────────────
        SettingsSection(title = "CATEGORIES & RULES")

        SettingsCard {
            SettingsRow(
                label = "Categories",
                subtitle = null,
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (categoryCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "$categoryCount",
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KaasuColors.forest
                                    )
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                onClick = onNavigateToCategories,
                isLast = false
            )
            SettingsRow(
                label = "Merchant rules",
                subtitle = if (ruleCount > 0) "$ruleCount auto-tags" else "No rules yet",
                onClick = onNavigateToMerchantRules,
                isLast = false
            )
            SettingsRow(
                label = "Budgets",
                subtitle = "Monthly limits by category",
                onClick = onNavigateToBudgets,
                isLast = false
            )
            SettingsRow(
                label = "Subscriptions",
                subtitle = "Auto-detected recurring payments",
                onClick = onNavigateToSubscriptions,
                isLast = false
            )
            SettingsRow(
                label = "Needs a tag",
                subtitle = "Review uncategorized transactions",
                onClick = onNavigateToNeedsTag,
                isLast = false
            )
            SettingsRow(
                label = "Duplicates",
                subtitle = "Review transactions marked as duplicate",
                onClick = onNavigateToDuplicates,
                isLast = false
            )
            SettingsRow(
                label = "Currency",
                subtitle = "₹ INR",
                onClick = null,
                isLast = true
            )
        }

        // ── DATA ──────────────────────────────────────────────────────────────
        SettingsSection(title = "DATA")

        SettingsCard {
            IconSettingsRow(
                icon = Icons.Default.Save,
                label = "Back up data",
                subtitle = "Save everything to a .json file",
                onClick = { viewModel.backupData(context) },
                isLast = false
            )
            IconSettingsRow(
                icon = Icons.Default.History,
                label = "Restore from backup",
                subtitle = "Replaces current data with a backup",
                onClick = { restorePicker.launch(arrayOf("application/json", "*/*")) },
                isLast = false
            )
            IconSettingsRow(
                icon = Icons.Default.Description,
                label = "Export transactions (CSV)",
                onClick = { viewModel.exportCsv(context) },
                isLast = false
            )
            IconSettingsRow(
                icon = Icons.Default.Upload,
                label = "Import bank statement",
                subtitle = "CSV, PDF or XLSX — IDFC FIRST, GPay, SBI, generic card",
                onClick = {
                    importStatementPicker.launch(
                        arrayOf(
                            "text/csv",
                            "application/pdf",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                    )
                },
                isLast = true
            )
        }

        // ── HELP & LEGAL ──────────────────────────────────────────────────────
        SettingsSection(title = "HELP & LEGAL")

        SettingsCard {
            IconSettingsRow(
                icon = Icons.Default.Shield,
                label = "Privacy policy",
                onClick = { onNavigateToLegal("privacy") },
                isLast = false
            )
            IconSettingsRow(
                icon = Icons.Default.Description,
                label = "Terms of service",
                onClick = { onNavigateToLegal("terms") },
                isLast = false
            )
            IconSettingsRow(
                icon = Icons.Default.Info,
                label = "About Kaasu",
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                onClick = { showAbout = true },
                isLast = false
            )
            IconSettingsRow(
                icon = Icons.AutoMirrored.Filled.Message,
                label = "Help & support",
                onClick = { showSupport = true },
                isLast = true
            )
        }

        // ── DANGER ZONE ───────────────────────────────────────────────────────
        // Red section header
        Text(
            text = "DANGER ZONE",
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = KaasuColors.expense
            ),
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 6.dp)
        )

        // Red-tinted card
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, Color(0xFFF0C8A0), RoundedCornerShape(16.dp))
        ) {
            // Clear all transactions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showClearDialog = true }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = KaasuColors.expense,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Clear all transactions",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = KaasuColors.expense
                    ),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = KaasuColors.expense,
                    modifier = Modifier.size(16.dp)
                )
            }

            HorizontalDivider(color = Color(0xFFF8E2C4), modifier = Modifier.padding(horizontal = 14.dp))

            // Delete account & wipe
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToPrivacy)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = KaasuColors.expense,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Delete account & wipe device",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = KaasuColors.expense
                    ),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = KaasuColors.expense,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // ── Footer ────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Kaasu · v${BuildConfig.VERSION_NAME}",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "made in Chennai",
                style = TextStyle(
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable composables
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Section label: 11sp, bold, uppercase, letterSpacing 1sp,
 * padding 18dp top, 24dp H, 6dp bottom.
 */
@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        style = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 6.dp)
    )
}

/**
 * Section label row with an optional tappable right-hand action label.
 */
@Composable
private fun SettingsSectionRow(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = KaasuColors.forest
                ),
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

/**
 * Surface card container — margin 0 16dp, border, rounded 16dp.
 * Inner rows handle their own vertical padding.
 */
@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        content()
    }
}

/**
 * A standard row inside a SettingsCard with an optional subtitle and custom
 * trailing content. The divider is suppressed for the last row.
 */
@Composable
private fun ToggleRow(
    label: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = KaasuColors.onForest,
                checkedTrackColor = KaasuColors.forest,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
private fun SettingsRow(
    label: String,
    subtitle: String?,
    onClick: (() -> Unit)?,
    isLast: Boolean,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            if (trailingContent != null) {
                trailingContent()
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (!isLast) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }
    }
}

/**
 * Settings row with a leading icon, label, optional subtitle, and custom or default trailing.
 */
@Composable
private fun IconSettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String? = null,
    onClick: (() -> Unit)?,
    isLast: Boolean,
    trailing: @Composable (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (!isLast) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }
    }
}

/**
 * Three-pill theme picker: System / Light / Dark.
 * Selected pill: KaasuColors.forest bg + KaasuColors.onForest text.
 * Inactive pill: surface bg + outline border + onSurface text (theme-aware).
 */
@Composable
private fun ThemePicker(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        SettingsDataStore.Theme.SYSTEM to "System",
        SettingsDataStore.Theme.LIGHT  to "Light",
        SettingsDataStore.Theme.DARK   to "Dark"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = selected == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) KaasuColors.forest else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) KaasuColors.forest else MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(value) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) KaasuColors.onForest
                                else MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

/**
 * Privacy hero chip — semi-transparent pill.
 * Always on a KaasuColors.forest background, so uses KaasuColors.onForest throughout.
 */
@Composable
private fun PrivacyChip(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(KaasuColors.onForest.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = KaasuColors.onForest
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Budget dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BudgetDialog(
    currentBudgetInPaise: Long,
    onConfirm: (Long) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember {
        mutableStateOf(
            if (currentBudgetInPaise > 0) currentBudgetInPaise.toAmountDisplay() else ""
        )
    }
    val isValid = input.parseToPaise() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Monthly budget") },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Amount (₹)") },
                placeholder = { Text("e.g. 20000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = input.isNotBlank() && !isValid
            )
        },
        confirmButton = {
            TextButton(
                onClick = { input.parseToPaise()?.let { onConfirm(it) } },
                enabled = isValid
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (currentBudgetInPaise > 0) {
                    TextButton(onClick = onClear) { Text("Clear") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

// Opens an email draft to support with a diagnostics footer (no personal data).
private fun android.content.Context.sendSupportEmail(subject: String) {
    val body = "\n\n\n— — —\n${diagnosticsId()}"
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = android.net.Uri.parse("mailto:support@kaasu.app")
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    runCatching { startActivity(intent) }
}


/** Compact "3 days ago" style label for the capture-health rows. */
private fun relativeTimeShort(millis: Long): String {
    val minutes = (System.currentTimeMillis() - millis) / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        else -> "${minutes / (60 * 24)}d ago"
    }
}
