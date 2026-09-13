package com.kaasu.app.feature.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaasu.app.ui.theme.KaasuColors
import com.kaasu.app.accessibility.AccessibilityServiceStatus
import com.kaasu.app.core.util.parseToPaise
import com.kaasu.app.sms.worker.SmsBackfillWorker
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme

@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.isDone) {
        if (state.isDone) onContinue()
    }
    LaunchedEffect(Unit) {
        viewModel.message.collect { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    val restorePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.restoreBackup(it) } }

    AnimatedContent(
        targetState = state.page,
        transitionSpec = {
            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
        },
        label = "onboarding_page",
        modifier = Modifier
            .fillMaxSize()
            // The page background, so it must be a background role. This used to be `onForest` —
            // the cream that sits *on* the green — which only looked right in light mode. In dark
            // mode it resolves near-white and the headings, which use `ink` (also near-white in
            // dark), disappeared into it entirely.
            .background(MaterialTheme.colorScheme.background)
    ) { page ->
        when (page) {
            0 -> WelcomePage(
                onNext = viewModel::nextPage,
                onRestore = { restorePicker.launch(arrayOf("application/json", "*/*")) }
            )
            1 -> NotificationPage(onNext = viewModel::nextPage)
            2 -> SmsPage(onNext = viewModel::nextPage)
            3 -> AccessibilityPage(onNext = viewModel::nextPage)
            else -> BudgetPage(
                input = state.budgetInput,
                onInputChange = viewModel::onBudgetInputChange,
                onComplete = viewModel::complete
            )
        }
    }
}

// ── Screen 01: Welcome ────────────────────────────────────────────────────────
@Composable
private fun WelcomePage(onNext: () -> Unit, onRestore: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        // K logo chip
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(KaasuColors.forest),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "K",
                color = KaasuColors.onForest,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                letterSpacing = (-1).sp
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "Kaasu",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.2).sp,
            color = KaasuColors.ink
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "A spending tracker that types itself.",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = KaasuColors.muted,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(44.dp))

        // Feature rows
        FeatureRow(
            icon = Icons.Default.Chat,
            title = "Reads your bank's notifications",
            subtitle = "Auto-logs every debit, credit, UPI."
        )
        Spacer(Modifier.height(22.dp))
        FeatureRow(
            icon = Icons.Default.Lock,
            title = "Stays on your phone",
            subtitle = "No accounts, no cloud, no telemetry."
        )
        Spacer(Modifier.height(22.dp))
        FeatureRow(
            icon = Icons.Default.BarChart,
            title = "Sees patterns, not you",
            subtitle = "Categories, budgets, no profiles."
        )

        Spacer(Modifier.weight(1f))

        // CTA button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(KaasuColors.forest)
                .clickable(onClick = onNext),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Set up Kaasu",
                color = KaasuColors.onForest,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = "Already a user? Restore from backup",
            fontSize = 13.sp,
            color = KaasuColors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable(onClick = onRestore)
        )
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(KaasuColors.surfaceAlt),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = KaasuColors.forest,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = KaasuColors.ink
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = KaasuColors.muted
            )
        }
    }
}

// ── Screen 02: Notification permission ───────────────────────────────────────
@Composable
private fun NotificationPage(onNext: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isGranted by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGranted = NotificationManagerCompat
                    .getEnabledListenerPackages(context)
                    .contains(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 28.dp)
    ) {
        // Step progress bar: 6 segments total (Welcome, Notification, Sms, Accessibility, Budget + 1 for balance), 2 active here
        StepProgressBar(activeSegments = 2, totalSegments = 6)

        Spacer(Modifier.height(28.dp))

        // Bell icon
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(KaasuColors.forest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = KaasuColors.onForest,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Let Kaasu read your bank alerts",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.2).sp,
            lineHeight = 34.sp,
            color = KaasuColors.ink
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "We listen to notifications from your banks to log transactions automatically. The text never leaves your device.",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 22.sp,
            color = KaasuColors.muted
        )

        Spacer(Modifier.height(24.dp))

        // Sample notification card
        SampleNotifCard()

        Spacer(Modifier.height(14.dp))

        // "Becomes 1 logged expense" indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(KaasuColors.surfaceAlt),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✓", color = KaasuColors.forest, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Text(
                text = "Becomes 1 logged expense. Nothing else.",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = KaasuColors.muted
            )
        }

        Spacer(Modifier.weight(1f))

        // Permission status dot
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) KaasuColors.income else KaasuColors.muted)
            )
            Text(
                text = if (isGranted) "Notification access enabled" else "Not enabled yet",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isGranted) KaasuColors.income else KaasuColors.muted
            )
        }

        if (isGranted) {
            CtaButton(text = "Continue", onClick = onNext)
        } else {
            CtaButton(
                text = "Allow notification access",
                onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onNext),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "What we read, exactly",
                    color = KaasuColors.forest,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// Shared step-progress indicator used by NotificationPage and SmsPage.
@Composable
private fun StepProgressBar(activeSegments: Int, totalSegments: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(totalSegments) { idx ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (idx < activeSegments) KaasuColors.forest else KaasuColors.surfaceAlt)
            )
        }
    }
}

// ── Screen 03: SMS permission (optional, additive capture channel) ──────────
@Composable
private fun SmsPage(onNext: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.RECEIVE_SMS] == true &&
            grants[Manifest.permission.READ_SMS] == true
        isGranted = granted
        if (granted) {
            SmsBackfillWorker.enqueue(context)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                    PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 28.dp)
    ) {
        StepProgressBar(activeSegments = 3, totalSegments = 6)

        Spacer(Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(KaasuColors.forest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Message,
                contentDescription = null,
                tint = KaasuColors.onForest,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Also read bank SMS directly",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.2).sp,
            lineHeight = 34.sp,
            color = KaasuColors.ink
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Optional. Some banks only send SMS, not app notifications. Kaasu can also read " +
                "your SMS inbox to catch those — including a one-time scan of your existing messages. " +
                "Entirely on-device.",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 22.sp,
            color = KaasuColors.muted
        )

        Spacer(Modifier.height(24.dp))
        SampleNotifCard()

        Spacer(Modifier.weight(1f))

        // Permission status dot
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) KaasuColors.income else KaasuColors.muted)
            )
            Text(
                text = if (isGranted) "SMS access enabled" else "Not enabled — notifications will still work",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isGranted) KaasuColors.income else KaasuColors.muted
            )
        }

        if (isGranted) {
            CtaButton(text = "Continue", onClick = onNext)
        } else {
            CtaButton(
                text = "Allow SMS access",
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                    )
                }
            )
        }

        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onNext),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Skip — use notifications only",
                color = KaasuColors.forest,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

// ── Screen 04: Accessibility screen-reading (optional, lowest-priority capture channel) ─────
// Modeled directly on SmsPage: same "always show a Skip action, degrade gracefully" pattern, since
// this channel is even more optional than SMS was — it only ever catches what the other three
// channels missed. Uses Settings.ACTION_ACCESSIBILITY_SETTINGS (a special-access Settings screen,
// same category as ACTION_NOTIFICATION_LISTENER_SETTINGS on NotificationPage) rather than a runtime
// permission dialog — AccessibilityService is never part of the dangerous-permission model.
@Composable
private fun AccessibilityPage(onNext: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isGranted by remember { mutableStateOf(AccessibilityServiceStatus.isEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGranted = AccessibilityServiceStatus.isEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 28.dp)
    ) {
        StepProgressBar(activeSegments = 4, totalSegments = 6)

        Spacer(Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(KaasuColors.forest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Visibility,
                contentDescription = null,
                tint = KaasuColors.onForest,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Screen reading (not working yet)",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.2).sp,
            lineHeight = 34.sp,
            color = KaasuColors.ink
        )
        Spacer(Modifier.height(14.dp))
        Text(
            // Honest copy beats a good pitch here. Tested against current Google Pay, this channel
            // captures nothing: GPay exposes no text nodes and a single view id to the accessibility
            // tree, so the scraper's screen detection can never match and it would read no text even
            // if it did. Asking for Android's most powerful permission class on the strength of a
            // feature that does not function would be the worst trade in this app.
            text = "This would let Kaasu read your Google Pay or PhonePe transaction history " +
                "on screen, to catch a payment the other methods missed.\n\n" +
                "It does not currently work — those apps no longer expose their screen text in a " +
                "way Kaasu can read. Turning it on grants Android's most powerful permission and " +
                "gains you nothing today, so leave it off. Notifications, SMS and statement import " +
                "are unaffected.",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 22.sp,
            color = KaasuColors.muted
        )

        Spacer(Modifier.weight(1f))

        // Permission status dot
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) KaasuColors.income else KaasuColors.muted)
            )
            Text(
                text = if (isGranted) "Enabled — but it captures nothing today" else "Off — recommended",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isGranted) KaasuColors.income else KaasuColors.muted
            )
        }

        if (isGranted) {
            CtaButton(text = "Continue", onClick = onNext)
        } else {
            CtaButton(
                text = "Turn it on anyway",
                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            )
        }

        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onNext),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Skip — recommended",
                color = KaasuColors.forest,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun SampleNotifCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, KaasuColors.border, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(KaasuColors.forest),
                contentAlignment = Alignment.Center
            ) {
                Text("HD", color = KaasuColors.onForest, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
            }
            Text(
                text = "HDFC BANK",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = KaasuColors.ink,
                letterSpacing = 0.5.sp,
                modifier = Modifier.weight(1f)
            )
            Text(text = "just now", fontSize = 11.sp, color = KaasuColors.muted, fontWeight = FontWeight.SemiBold)
        }
        Text(
            text = "Sent Rs 540.00 from A/c ·· 4823 to SWIGGY on 12-Jun-26. UPI Ref 318...",
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = KaasuColors.ink,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}

// ── Screen 03: Budget setup ───────────────────────────────────────────────────
@Composable
private fun BudgetPage(
    input: String,
    onInputChange: (String) -> Unit,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            text = "Set a monthly budget",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.8).sp,
            textAlign = TextAlign.Center,
            color = KaasuColors.ink
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Optional — helps Kaasu warn you before you overspend. Change it any time in Settings.",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = KaasuColors.muted
        )

        Spacer(Modifier.height(36.dp))

        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            label = { Text("Monthly budget (₹)") },
            placeholder = { Text("e.g. 60000") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = input.isNotBlank() && input.parseToPaise() == null,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.weight(1f))

        CtaButton(text = "Start tracking", onClick = onComplete)
    }
}

@Composable
private fun CtaButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(KaasuColors.forest)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = KaasuColors.onForest,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}
