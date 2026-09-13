package com.kaasu.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaasu.app.ui.theme.KaasuColors
import com.kaasu.app.core.util.formatRupees
import com.kaasu.app.core.util.toComposeColor
import com.kaasu.app.domain.model.Category
import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionCard(
    transaction: Transaction,
    category: Category?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accountName: String? = null,
    lastFourDigits: String? = null
) {
    val timeLabel = SimpleDateFormat("h:mm a", LocalConfiguration.current.locales[0])
        .format(Date(transaction.transactionTime))
    val (chipBg, chipText, amountColor, amountPrefix) = resolveColors(transaction.type, category)
    val initial = (transaction.merchantName?.firstOrNull()?.uppercaseChar()
        ?: transaction.type.name.first())

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, KaasuColors.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Colored initial chip
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(chipBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial.toString(),
                color = chipText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = transaction.merchantName ?: "Unknown",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = KaasuColors.ink,
                maxLines = 1
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                category?.let {
                    Text(
                        text = it.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = KaasuColors.muted,
                        fontSize = 12.sp
                    )
                    Text(text = "·", style = MaterialTheme.typography.labelSmall, color = KaasuColors.muted, fontSize = 12.sp)
                }
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = KaasuColors.muted,
                    fontSize = 12.sp
                )
            }
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "$amountPrefix${transaction.amountInPaise.formatRupees().removePrefix("₹")}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = amountColor
            )
            lastFourDigits?.let {
                Text(text = "·· $it", style = MaterialTheme.typography.labelSmall, color = KaasuColors.muted, fontSize = 10.sp)
            } ?: accountName?.let {
                Text(text = it, style = MaterialTheme.typography.labelSmall, color = KaasuColors.muted, fontSize = 10.sp)
            }
        }
    }
}

private data class ChipColors(
    val chipBg: Color,
    val chipText: Color,
    val amountColor: Color,
    val amountPrefix: String
)

// Reads the brand palette, so it has to run in composition rather than as a plain helper —
// that is precisely how these colors ended up pinned to the light theme before.
@Composable
private fun resolveColors(type: TransactionType, category: Category?): ChipColors {
    val (amountColor, prefix) = when (type) {
        TransactionType.EXPENSE  -> KaasuColors.expense to "−₹"
        TransactionType.INCOME   -> KaasuColors.income  to "+₹"
        TransactionType.TRANSFER -> KaasuColors.transfer to "₹"
        TransactionType.REFUND, TransactionType.CASHBACK -> KaasuColors.income to "+₹"
        TransactionType.UNKNOWN  -> Color.Gray   to "₹"
    }

    val parsedColor = category?.color?.let {
        runCatching { it.toComposeColor() }.getOrNull()
    }

    val chipBg = when {
        parsedColor != null -> parsedColor.copy(alpha = 0.15f)
        type == TransactionType.EXPENSE  -> KaasuColors.amberBg
        type == TransactionType.INCOME   -> KaasuColors.income.copy(alpha = 0.15f)
        else -> KaasuColors.transfer.copy(alpha = 0.15f)
    }
    val chipText = when {
        parsedColor != null -> parsedColor
        type == TransactionType.EXPENSE  -> KaasuColors.amber
        type == TransactionType.INCOME   -> KaasuColors.income
        else -> KaasuColors.transfer
    }

    return ChipColors(chipBg, chipText, amountColor, prefix)
}
