package com.kaasu.app.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaasu.app.ui.theme.KaasuColors
import com.kaasu.app.core.util.formatRupees
import com.kaasu.app.core.util.toComposeColor
import com.kaasu.app.domain.model.Category
import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.core.util.toAmountDisplay
import com.kaasu.app.domain.model.Account
import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.domain.repository.SplitSlice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSplit by remember { mutableStateOf(false) }
    var showTransferSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onBack()
    }
    LaunchedEffect(state.isSplit) {
        if (state.isSplit) onBack()
    }
    LaunchedEffect(state.isMarkedDuplicate) {
        if (state.isMarkedDuplicate) onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            state.isLoading -> Unit
            state.transaction == null -> {
                Text(
                    text = "Transaction not found",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
            else -> {
                val tx = state.transaction!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── HEADER ROW ───────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeaderIconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        HeaderIconButton(onClick = { onEdit(tx.id) }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // ── HERO CARD ────────────────────────────────────────────
                    HeroCard(tx = tx, category = state.category)

                    // ── DETAILS CARD ─────────────────────────────────────────
                    DetailsCard(tx = tx, accountName = state.account?.displayName)

                    // ── NOTIFICATION TEXT CARD ───────────────────────────────
                    val rawText = state.capturedText
                    if (!rawText.isNullOrBlank()) {
                        NotificationTextCard(rawText = rawText)
                    }

                    // ── HOW OFTEN ────────────────────────────────────────────
                    // Whether this is a habit or a one-off, which a single row cannot say. The
                    // difference between a ₹300 coffee and ₹2,400 of coffee.
                    state.merchantVisits?.takeIf { it.isRepeat }?.let { visits ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(KaasuColors.forest.copy(alpha = 0.08f))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${visits.totalVisits} visits",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KaasuColors.ink
                                )
                                Text(
                                    text = if (visits.visitsInWindow > 0) {
                                        "${visits.spentInWindowInPaise.toAmountDisplay()} this month · " +
                                            "${visits.averageSpendInPaise.toAmountDisplay()} avg"
                                    } else {
                                        "${visits.totalSpentInPaise.toAmountDisplay()} all time · " +
                                            "${visits.averageSpendInPaise.toAmountDisplay()} avg"
                                    },
                                    fontSize = 12.sp,
                                    color = KaasuColors.muted
                                )
                            }
                        }
                    }

                    // ── ACTION BUTTONS ───────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionButton(
                            label = if (tx.isRecurring) "✓ Recurring" else "Mark recurring",
                            active = tx.isRecurring,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.toggleRecurring() }
                        )
                        ActionButton(
                            label = "Split expense",
                            modifier = Modifier.weight(1f),
                            onClick = { showSplit = true }
                        )
                    }

                    // ── NOT SPENDING AT ALL ──────────────────────────────────
                    // A card bill or a move between the owner's accounts is not a purchase.
                    // Counting it as one is what made a ₹2,399 subscription read as ₹4,798.
                    if (tx.transferGroupId == null && tx.type != TransactionType.TRANSFER) {
                        ActionButton(
                            label = "Move between my accounts",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                            onClick = { showTransferSheet = true }
                        )
                    }

                    // ── MARK AS DUPLICATE ────────────────────────────────────
                    ActionButton(
                        label = "Mark as duplicate",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        onClick = { viewModel.markDuplicate() }
                    )

                    // ── DELETE BUTTON ────────────────────────────────────────
                    TextButton(
                        onClick = {
                            viewModel.deleteTransaction()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Delete transaction",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = KaasuColors.expense,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    val tx = state.transaction

    if (showTransferSheet && tx != null) {
        MarkAsTransferSheet(
            merchantName = tx.merchantName,
            // Money cannot arrive in the account it left.
            accounts = state.accounts.filter { it.id != tx.accountId },
            onDismiss = { showTransferSheet = false },
            onConfirm = { accountId, remember ->
                showTransferSheet = false
                viewModel.markAsTransfer(accountId, remember)
            }
        )
    }

    if (showSplit && tx != null) {
        SplitSheet(
            totalInPaise = tx.amountInPaise,
            categories = state.categories,
            onDismiss = { showSplit = false },
            onConfirm = { slices ->
                showSplit = false
                viewModel.splitTransaction(slices)
            }
        )
    }
}

// ── Header icon button ─────────────────────────────────────────────────────────

@Composable
private fun HeaderIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(36.dp)
        ) {
            content()
        }
    }
}

// ── Hero card ─────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(tx: Transaction, category: Category?) {
    val categoryColor = category?.color?.toComposeColor() ?: MaterialTheme.colorScheme.onSurfaceVariant
    val amountColor = when (tx.type) {
        TransactionType.INCOME, TransactionType.REFUND, TransactionType.CASHBACK -> KaasuColors.income
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface
    }
    val prefixSymbol = when (tx.type) {
        TransactionType.EXPENSE -> "−₹"
        TransactionType.INCOME, TransactionType.REFUND, TransactionType.CASHBACK -> "+₹"
        else -> "₹"
    }
    val amountWithoutSymbol = tx.amountInPaise.formatRupees().removePrefix("₹")

    val dateFormat = SimpleDateFormat("EEEE, d MMM yyyy", LocalConfiguration.current.locales[0])
    val formattedDate = dateFormat.format(Date(tx.transactionTime))

    val merchantInitial = (tx.merchantName ?: "?").firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .padding(22.dp)
    ) {
        // Merchant row: chip + name + type badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Category initial chip
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = merchantInitial,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = categoryColor
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = tx.merchantName ?: "Unknown",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.width(8.dp))

            // Type badge
            TypeBadge(type = tx.type)
        }

        Spacer(Modifier.height(12.dp))

        // Amount row
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = prefixSymbol,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 54.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = amountWithoutSymbol,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-2).sp,
                color = amountColor,
                lineHeight = 54.sp
            )
        }

        Spacer(Modifier.height(6.dp))

        // Date
        Text(
            text = formattedDate,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(10.dp))

        // Category badge
        if (category != null) {
            Box(
                modifier = Modifier
                    .background(categoryColor.copy(alpha = 0.12f), RoundedCornerShape(50.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = category.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = categoryColor
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(50.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Uncategorized",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TypeBadge(type: TransactionType) {
    val (label, color) = when (type) {
        TransactionType.EXPENSE -> "Expense" to KaasuColors.expense
        TransactionType.INCOME -> "Income" to KaasuColors.income
        TransactionType.TRANSFER -> "Transfer" to KaasuColors.forest
        TransactionType.REFUND -> "Refund" to KaasuColors.income
        TransactionType.CASHBACK -> "Cashback" to KaasuColors.income
        TransactionType.UNKNOWN -> "Unknown" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ── Details card ──────────────────────────────────────────────────────────────

@Composable
private fun DetailsCard(tx: Transaction, accountName: String?) {
    val rows = buildList {
        accountName?.let { add("Account" to it) }
        tx.paymentMode?.let { add("Mode" to it) }
        tx.rawTextHash?.let { add("Reference" to it) }
        tx.sourceAppName?.let { add("Source" to it) }
        if (tx.isManual) add("Entry" to "Manual")
        tx.note?.let { add("Note" to it) }
    }

    if (rows.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 0.dp)
    ) {
        Text(
            text = "DETAILS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 14.dp, bottom = 10.dp)
        )

        rows.forEachIndexed { index, (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                val isReference = label == "Reference"
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = if (isReference) FontFamily.Monospace else FontFamily.Default,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .weight(2f)
                        .padding(start = 8.dp)
                )
            }
            if (index < rows.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

// ── Notification text card ────────────────────────────────────────────────────

@Composable
private fun NotificationTextCard(rawText: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "ORIGINAL NOTIFICATION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = rawText,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}

// ── Action button ─────────────────────────────────────────────────────────────

@Composable
private fun ActionButton(
    label: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(
                if (active) KaasuColors.forest.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(14.dp)
            )
            .border(
                1.dp,
                if (active) KaasuColors.forest else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (active) KaasuColors.forest else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ── Split sheet ─────────────────────────────────────────────────────────────────

private class SplitRow(val categoryId: Long?, val amountText: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitSheet(
    totalInPaise: Long,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (List<SplitSlice>) -> Unit
) {
    // Start with two slices: the full amount in the first, zero in the second.
    val rows = remember {
        listOf(
            SplitRow(null, (totalInPaise / 100.0).let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }),
            SplitRow(null, "")
        ).toMutableStateList()
    }

    fun enteredPaise(): Long = rows.sumOf { (it.amountText.toDoubleOrNull() ?: 0.0).times(100).toLong() }
    val remaining = totalInPaise - enteredPaise()
    val balanced = remaining == 0L && rows.count { (it.amountText.toDoubleOrNull() ?: 0.0) > 0 } >= 2

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Split ${totalInPaise.formatRupees()}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = if (remaining == 0L) "Balanced" else if (remaining > 0) "${remaining.formatRupees()} left to assign" else "Over by ${(-remaining).formatRupees()}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (remaining == 0L) KaasuColors.income else KaasuColors.expense
            )

            rows.forEachIndexed { index, row ->
                SplitRowEditor(
                    row = row,
                    categories = categories,
                    onCategory = { rows[index] = SplitRow(it, row.amountText) },
                    onAmount = { rows[index] = SplitRow(row.categoryId, it) },
                    onRemove = if (rows.size > 2) ({ rows.removeAt(index) }) else null
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { rows.add(SplitRow(null, "")) }
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = KaasuColors.forest, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add slice", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KaasuColors.forest)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (balanced) KaasuColors.forest else KaasuColors.forest.copy(alpha = 0.4f))
                    .clickable(enabled = balanced) {
                        onConfirm(rows.map {
                            SplitSlice(
                                amountInPaise = (it.amountText.toDoubleOrNull() ?: 0.0).times(100).toLong(),
                                categoryId = it.categoryId
                            )
                        }.filter { it.amountInPaise > 0 })
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Split into ${rows.count { (it.amountText.toDoubleOrNull() ?: 0.0) > 0 }}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = KaasuColors.onForest)
            }
        }
    }
}

@Composable
private fun SplitRowEditor(
    row: SplitRow,
    categories: List<Category>,
    onCategory: (Long?) -> Unit,
    onAmount: (String) -> Unit,
    onRemove: (() -> Unit)?
) {
    val selected = categories.firstOrNull { it.id == row.categoryId }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("₹", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            BasicTextField(
                value = row.amountText,
                onValueChange = onAmount,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(KaasuColors.forest),
                modifier = Modifier.weight(1f)
            )
            if (onRemove != null) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp).clickable(onClick = onRemove)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.take(8).forEach { category ->
                val isSel = category.id == selected?.id
                val dot = category.color?.let { runCatching { it.toComposeColor() }.getOrNull() } ?: KaasuColors.forest
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .then(
                            if (isSel) Modifier.background(KaasuColors.forest)
                            else Modifier.background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(100.dp))
                        )
                        .clickable { onCategory(category.id) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(if (isSel) KaasuColors.onForest else dot))
                    Spacer(Modifier.width(5.dp))
                    Text(category.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isSel) KaasuColors.onForest else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

/**
 * Asks where the money actually went, and offers to remember the answer.
 *
 * "Where did this go?" rather than "is this a transfer?" — the owner knows the destination; the word
 * transfer is our vocabulary, not theirs. The remember option is what makes this a one-time
 * correction instead of a monthly chore: it writes a rule that catches the same payment next month.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkAsTransferSheet(
    merchantName: String?,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (accountId: Long?, rememberMerchant: Boolean) -> Unit
) {
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var alwaysApply by remember { mutableStateOf(merchantName?.isNotBlank() == true) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Where did this money go?",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = KaasuColors.ink
            )
            Text(
                text = "Moving money between your own accounts, or paying a credit-card bill, " +
                    "isn't spending — it stops counting toward your month.",
                fontSize = 13.sp,
                color = KaasuColors.muted
            )

            if (accounts.isEmpty()) {
                Text(
                    text = "No other account to move it to yet. Add one in Settings → Accounts.",
                    fontSize = 13.sp,
                    color = KaasuColors.muted
                )
            }

            accounts.forEach { account ->
                val isSelected = selectedId == account.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) KaasuColors.forest.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surface
                        )
                        .border(
                            1.dp,
                            if (isSelected) KaasuColors.forest else KaasuColors.border,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedId = account.id }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = account.lastFourDigits
                            ?.let { "${account.displayName} ·· $it" }
                            ?: account.displayName,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = KaasuColors.ink,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Text(text = "✓", fontSize = 15.sp, color = KaasuColors.forest)
                    }
                }
            }

            if (!merchantName.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { alwaysApply = !alwaysApply }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(checked = alwaysApply, onCheckedChange = { alwaysApply = it })
                    Text(
                        text = "Always treat \"$merchantName\" this way",
                        fontSize = 13.sp,
                        color = KaasuColors.ink,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            ActionButton(
                label = "Save",
                active = true,
                modifier = Modifier.fillMaxWidth(),
                // The destination is optional: knowing it was not spending is already the fix, and
                // insisting on an account the owner may not track would leave the total wrong.
                onClick = { onConfirm(selectedId, alwaysApply) }
            )
        }
    }
}
