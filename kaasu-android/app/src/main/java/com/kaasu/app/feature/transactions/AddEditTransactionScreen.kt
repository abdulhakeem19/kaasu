package com.kaasu.app.feature.transactions

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaasu.app.ui.theme.KaasuColors
import com.kaasu.app.core.util.toComposeColor
import com.kaasu.app.domain.model.Account
import com.kaasu.app.domain.model.Category
import com.kaasu.app.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun AddEditTransactionScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddEditViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Local numpad state — raw string of digits and optional decimal point
    var rawInput by rememberSaveable { mutableStateOf("0") }

    // Sync rawInput → ViewModel on every change
    LaunchedEffect(rawInput) {
        val paise = rawInput.toDoubleOrNull()?.times(100)?.toLong() ?: 0L
        viewModel.onAmountChange(rawInput)
        // amountText in the ViewModel is kept in sync; paise conversion happens at save()
    }

    // Pre-fill rawInput when editing an existing transaction
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading && state.isEditMode && rawInput == "0") {
            rawInput = state.amountText.ifBlank { "0" }
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }

    val isSaveEnabled = rawInput != "0" && rawInput.isNotBlank() && rawInput.toDoubleOrNull() != null

    var showAccountPicker by remember { mutableStateOf(false) }
    var showToAccountPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val selectedAccount = state.accounts.firstOrNull { it.id == state.accountId }
    val selectedToAccount = state.accounts.firstOrNull { it.id == state.toAccountId }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Scrollable content ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                HeaderRow(
                    isEditMode = state.isEditMode,
                    isSaveEnabled = isSaveEnabled,
                    onBack = onBack,
                    onSave = viewModel::save
                )

                // Type toggle
                TypeToggle(
                    selected = state.type,
                    onSelect = viewModel::onTypeChange
                )

                // Amount display
                AmountDisplay(rawInput = rawInput)

                // Category pills
                if (state.categories.isNotEmpty()) {
                    CategoryPills(
                        categories = state.categories,
                        selectedId = state.categoryId,
                        onSelect = viewModel::onCategoryChange,
                        onMoreClick = { showCategoryPicker = true }
                    )
                }

                // Details card
                DetailsCard(
                    merchantName = state.merchantName,
                    accountLabel = selectedAccount?.let { acc ->
                        acc.lastFourDigits?.let { "${acc.displayName} ·· $it" } ?: acc.displayName
                    },
                    transactionTime = state.transactionTime,
                    note = state.note,
                    onMerchantChange = viewModel::onMerchantChange,
                    onAccountClick = { showAccountPicker = true },
                    onDateClick = { showDatePicker = true },
                    onNoteChange = viewModel::onNoteChange,
                    isTransfer = state.isTransfer,
                    toAccountLabel = selectedToAccount?.let { acc ->
                        acc.lastFourDigits?.let { "${acc.displayName} ·· $it" } ?: acc.displayName
                    },
                    onToAccountClick = { showToAccountPicker = true },
                )

                // Offer to remember the rename for this merchant everywhere + future captures
                if (state.showRenameOption) {
                    RenameEverywhereRow(
                        original = state.originalMerchant,
                        checked = state.renameEverywhere,
                        onCheckedChange = viewModel::onRenameEverywhereChange
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }

            // ── Pinned numpad ─────────────────────────────────────────────────
            Numpad(
                rawInput = rawInput,
                onRawInputChange = { rawInput = it }
            )
        }
    }

    if (showAccountPicker) {
        AccountPickerSheet(
            accounts = state.accounts,
            selectedId = state.accountId,
            onSelect = {
                viewModel.onAccountChange(it)
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false }
        )
    }

    if (showToAccountPicker) {
        AccountPickerSheet(
            // The account the money left is not somewhere it can also arrive.
            accounts = state.accounts.filter { it.id != state.accountId },
            selectedId = state.toAccountId,
            onSelect = {
                viewModel.onToAccountChange(it)
                showToAccountPicker = false
            },
            onDismiss = { showToAccountPicker = false }
        )
    }

    if (showCategoryPicker) {
        CategoryPickerSheet(
            categories = state.categories,
            selectedId = state.categoryId,
            onSelect = {
                viewModel.onCategoryChange(it)
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false }
        )
    }

    if (showDatePicker) {
        DatePickerModal(
            initialMillis = state.transactionTime,
            onConfirm = {
                viewModel.onDateChange(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

// ─── Header row ──────────────────────────────────────────────────────────────

@Composable
private fun HeaderRow(
    isEditMode: Boolean,
    isSaveEnabled: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Close button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onBack)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }

        // Title
        Text(
            text = if (isEditMode) "Edit transaction" else "Add expense",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        // Save pill
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSaveEnabled) KaasuColors.forest else KaasuColors.forest.copy(alpha = 0.4f))
                .clickable(enabled = isSaveEnabled, onClick = onSave)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Save",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = KaasuColors.onForest
            )
        }
    }
}

// ─── Type toggle ─────────────────────────────────────────────────────────────

@Composable
private fun TypeToggle(
    selected: TransactionType,
    onSelect: (TransactionType) -> Unit
) {
    val types = listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        types.forEach { type ->
            val isActive = selected == type
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .then(
                        if (isActive) Modifier.background(KaasuColors.forest)
                        else Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(100.dp))
                    )
                    .clickable { onSelect(type) }
            ) {
                Text(
                    text = type.label(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) KaasuColors.onForest else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ─── Amount display ───────────────────────────────────────────────────────────

@Composable
private fun AmountDisplay(rawInput: String) {
    // Blinking cursor animation
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorBlink"
    )

    // Format raw input for display (add thousand separators to integer part)
    val displayText = formatAmountDisplay(rawInput)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "₹",
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 2.dp, top = 12.dp)
        )
        Text(
            text = displayText,
            fontSize = 56.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = (-2).sp
        )
        Text(
            text = "|",
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .alpha(cursorAlpha)
                .padding(start = 2.dp, top = 8.dp)
        )
    }
}

/** Formats "1540.5" → "1,540.5" (no forced decimal padding for display) */
private fun formatAmountDisplay(raw: String): String {
    if (raw.isBlank() || raw == "0") return "0"
    val parts = raw.split(".")
    val intPart = parts[0].toLongOrNull() ?: return raw
    val formatted = "%,d".format(intPart)
    return if (parts.size > 1) "$formatted.${parts[1]}" else formatted
}

// ─── Category pills ───────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryPills(
    categories: List<Category>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onMoreClick: () -> Unit
) {
    val maxVisible = 5
    // Always keep the selected category visible even if it's outside the first five.
    val selected = categories.firstOrNull { it.id == selectedId }
    val head = categories.take(maxVisible).toMutableList()
    if (selected != null && head.none { it.id == selected.id }) {
        if (head.isNotEmpty()) head[head.lastIndex] = selected else head.add(selected)
    }
    val overflow = (categories.size - maxVisible).coerceAtLeast(0)

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        head.forEach { category ->
            CategoryPill(
                category = category,
                isSelected = category.id == selectedId,
                onClick = {
                    onSelect(if (category.id == selectedId) null else category.id)
                }
            )
        }
        if (overflow > 0) {
            OverflowPill(count = overflow, onClick = onMoreClick)
        }
    }
}

@Composable
private fun CategoryPill(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dotColor = category.color?.let { runCatching { it.toComposeColor() }.getOrNull() }
        ?: MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .then(
                if (isSelected) Modifier.background(KaasuColors.forest)
                else Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(100.dp))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isSelected) KaasuColors.onForest else dotColor)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = category.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) KaasuColors.onForest else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun OverflowPill(count: Int, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(100.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = "+$count More",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ─── Details card ─────────────────────────────────────────────────────────────

@Composable
private fun DetailsCard(
    merchantName: String,
    accountLabel: String?,
    transactionTime: Long,
    note: String,
    onMerchantChange: (String) -> Unit,
    onAccountClick: () -> Unit,
    onDateClick: () -> Unit,
    onNoteChange: (String) -> Unit,
    // A transfer has two ends and no payee, so it swaps the "Payee" row for a second account.
    isTransfer: Boolean = false,
    toAccountLabel: String? = null,
    onToAccountClick: () -> Unit = {},
) {
    val dateText = remember(transactionTime) {
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(transactionTime))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp)
    ) {
        // Moving money between your own accounts has no payee — asking for one invites a name
        // that then reads like a shop in the transaction list.
        if (!isTransfer) {
            EditableRow(
                label = "Payee",
                value = merchantName,
                placeholder = "Who / where",
                onValueChange = onMerchantChange,
                showDivider = true
            )
        }

        DetailsRow(
            label = if (isTransfer) "From" else "Account",
            value = accountLabel ?: "Select account",
            isPlaceholder = accountLabel == null,
            trailingContent = {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            },
            showDivider = true,
            onClick = onAccountClick
        )

        if (isTransfer) {
            DetailsRow(
                label = "To",
                value = toAccountLabel ?: "Select account",
                isPlaceholder = toAccountLabel == null,
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                showDivider = true,
                onClick = onToAccountClick
            )
        }

        // Date row — opens date picker
        DetailsRow(
            label = "Date",
            value = dateText,
            trailingContent = {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            },
            showDivider = true,
            onClick = onDateClick
        )

        // Note row — editable
        EditableRow(
            label = "Note",
            value = note,
            placeholder = "Add a note…",
            onValueChange = onNoteChange,
            showDivider = false
        )
    }
}

@Composable
private fun DetailsRow(
    label: String,
    value: String,
    trailingContent: @Composable () -> Unit,
    showDivider: Boolean,
    isPlaceholder: Boolean = false,
    onClick: (() -> Unit)?
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(72.dp)
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isPlaceholder) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            trailingContent()
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}

/** A details row whose value is an inline editable text field. */
@Composable
private fun EditableRow(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(72.dp)
            )
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(KaasuColors.forest),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}

// ─── Numpad ───────────────────────────────────────────────────────────────────

private val numpadKeys = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf(".", "0", "⌫")
)

@Composable
private fun Numpad(
    rawInput: String,
    onRawInputChange: (String) -> Unit
) {
    val hasDecimal = rawInput.contains('.')

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        numpadKeys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { key ->
                    NumpadKey(
                        key = key,
                        enabled = !(key == "." && hasDecimal),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onRawInputChange(processNumpadInput(rawInput, key))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NumpadKey(
    key: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (enabled) Modifier.clickable(onClick = onClick)
                else Modifier.alpha(0.3f)
            )
    ) {
        if (key == "⌫") {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = key,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Pure function: given current rawInput and a key press, returns the new rawInput. */
private fun processNumpadInput(current: String, key: String): String {
    return when (key) {
        "⌫" -> {
            if (current.length <= 1) "0"
            else current.dropLast(1).trimEnd().ifEmpty { "0" }
        }
        "." -> {
            if (current.contains('.')) current
            else "$current."
        }
        else -> {
            // digit key
            val decimalIndex = current.indexOf('.')
            if (decimalIndex != -1) {
                // Already has decimal — allow max 2 decimal places
                val decimals = current.length - decimalIndex - 1
                if (decimals >= 2) current else "$current$key"
            } else {
                // Integer part — max 8 digits
                val intPart = current.trimStart('0').ifEmpty { "0" }
                if (current == "0") key
                else if (intPart.length >= 8) current
                else "$current$key"
            }
        }
    }
}

// ─── Pickers ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountPickerSheet(
    accounts: List<Account>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Select account",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            PickerRow(label = "No account", selected = selectedId == null, onClick = { onSelect(null) })
            accounts.forEach { acc ->
                val label = acc.lastFourDigits?.let { "${acc.displayName} ·· $it" } ?: acc.displayName
                PickerRow(label = label, selected = acc.id == selectedId, onClick = { onSelect(acc.id) })
            }
            if (accounts.isEmpty()) {
                Text(
                    text = "No accounts yet. Add one in Settings → Accounts.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(
    categories: List<Category>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Select category",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            categories.forEach { category ->
                val dot = category.color?.let { runCatching { it.toComposeColor() }.getOrNull() }
                    ?: MaterialTheme.colorScheme.onSurfaceVariant
                PickerRow(
                    label = category.name,
                    selected = category.id == selectedId,
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(dot)
                        )
                    },
                    onClick = { onSelect(if (category.id == selectedId) null else category.id) }
                )
            }
        }
    }
}

@Composable
private fun PickerRow(
    label: String,
    selected: Boolean,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = KaasuColors.forest,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    initialMillis: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { datePickerState.selectedDateMillis?.let(onConfirm) ?: onDismiss() }) {
                Text("OK", color = KaasuColors.forest, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun TransactionType.label(): String = when (this) {
    TransactionType.EXPENSE -> "Expense"
    TransactionType.INCOME -> "Income"
    TransactionType.TRANSFER -> "Transfer"
    else -> name.lowercase().replaceFirstChar { it.uppercase() }
}

// ─── Rename-everywhere toggle ──────────────────────────────────────────────────

@Composable
private fun RenameEverywhereRow(
    original: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = KaasuColors.forest)
        )
        Text(
            text = "Rename \"$original\" everywhere & remember for future",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}
