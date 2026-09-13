package com.kaasu.app.feature.statement

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.statement.model.StatementLineItem
import java.time.format.DateTimeFormatter

/**
 * Settings → "Import bank statement" flow: shows a loading state while [ImportStatementViewModel]
 * parses the picked file and runs the two-tier dedup check, then a preview ("N found — M new, K
 * already tracked") with a scrollable list of the new line items and a "Commit import" button.
 * The preview-before-commit gate matters here since this merges into existing data (unlike the
 * fully destructive backup-restore flow).
 */
@Composable
fun ImportStatementScreen(
    onDone: () -> Unit,
    viewModel: ImportStatementViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy")

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .clickable(onClick = onDone),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text("Import statement", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        }

        when (val s = state) {
            is ImportStatementUiState.Loading -> LoadingContent()
            is ImportStatementUiState.Error -> ErrorContent(message = s.message, onDone = onDone)
            is ImportStatementUiState.Preview -> PreviewContent(
                bankDisplayName = s.result.bankDisplayName,
                totalFound = s.result.totalFound,
                newCount = s.result.newCount,
                duplicateCount = s.result.duplicateCount,
                items = s.result.newItems,
                dateFmt = dateFmt,
                onCommit = viewModel::commit
            )
            is ImportStatementUiState.Committed ->
                CommittedContent(insertedCount = s.insertedCount, onDone = onDone)
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize().padding(bottom = 64.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = KaasuColors.forest)
            Spacer(Modifier.width(12.dp))
            Text(
                "Reading statement…",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            message,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = KaasuColors.expense,
            modifier = Modifier.padding(bottom = 20.dp)
        )
        Button(onClick = onDone) { Text("Done") }
    }
}

@Composable
private fun CommittedContent(insertedCount: Int, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Imported $insertedCount transaction${if (insertedCount == 1) "" else "s"}.",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 20.dp)
        )
        Button(onClick = onDone) { Text("Done") }
    }
}

@Composable
private fun PreviewContent(
    bankDisplayName: String,
    totalFound: Int,
    newCount: Int,
    duplicateCount: Int,
    items: List<StatementLineItem>,
    dateFmt: DateTimeFormatter,
    onCommit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            Text(bankDisplayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KaasuColors.forest)
            Text(
                "$totalFound found — $newCount new, $duplicateCount already tracked (skipped)",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
        ) {
            if (items.isEmpty()) {
                item {
                    Text(
                        "Nothing new to import — every line in this statement is already tracked.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                    )
                }
            }
            items(items) { item -> StatementLineRow(item = item, dateFmt = dateFmt) }
        }

        if (items.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = onCommit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Commit import · $newCount transaction${if (newCount == 1) "" else "s"}")
                }
            }
        }
    }
}

@Composable
private fun StatementLineRow(item: StatementLineItem, dateFmt: DateTimeFormatter) {
    val amountColor = when (item.direction) {
        TransactionType.INCOME, TransactionType.CASHBACK, TransactionType.REFUND -> KaasuColors.income
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.description.ifBlank { "Unknown" },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
            Text(item.date.format(dateFmt), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Text(item.amountInPaise.formatRupees(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = amountColor)
    }
}
