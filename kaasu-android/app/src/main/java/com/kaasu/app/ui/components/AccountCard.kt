package com.kaasu.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaasu.app.core.bank.BankRegistry
import com.kaasu.app.core.util.toAmountDisplay
import com.kaasu.app.domain.model.AccountBalance
import com.kaasu.app.ui.theme.KaasuColors

/**
 * One account and what is in it.
 *
 * Shared by the accounts list and the dashboard so a balance cannot read one way in one place and
 * another somewhere else. Cards state an amount outstanding rather than a negative balance, and an
 * account whose opening balance was never set asks for it instead of claiming a confident ₹0.
 */
@Composable
fun AccountCard(
    balance: AccountBalance,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val account = balance.account ?: return
    val bank = BankRegistry.resolve(account.displayName)
    val chipColor = bank?.color ?: KaasuColors.forest

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(chipColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = bank?.monogram ?: BankRegistry.monogramFor(account.displayName),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = account.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = KaasuColors.ink,
                maxLines = 1,
            )
            account.lastFourDigits?.let {
                Text(
                    text = "•••• $it",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = KaasuColors.muted,
                )
            }
        }

        if (balance.isKnown) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (balance.isCreditCard) {
                        balance.outstandingInPaise.toAmountDisplay()
                    } else {
                        balance.balanceInPaise.toAmountDisplay()
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (balance.isCreditCard && balance.outstandingInPaise > 0) {
                        KaasuColors.expense
                    } else {
                        KaasuColors.ink
                    },
                )
                if (balance.isCreditCard) {
                    Text(
                        text = "outstanding",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = KaasuColors.muted,
                    )
                }
            }
        } else {
            Text(
                text = "Set balance",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = KaasuColors.forest,
            )
        }
    }
}
