package com.kaasu.app.feature.accounts

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaasu.app.core.bank.BankRegistry
import com.kaasu.app.core.util.toColorHex
import com.kaasu.app.core.util.toComposeColor
import com.kaasu.app.domain.model.Account
import com.kaasu.app.core.util.toAmountDisplay
import com.kaasu.app.ui.theme.KaasuColors
import com.kaasu.app.domain.model.AccountBalance
import com.kaasu.app.domain.model.AccountType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<Account?>(null) }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete account?") },
            text = {
                Text("\"${deleteTarget!!.displayName}\" will be removed. Past transactions linked to it will remain but lose the account tag.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(deleteTarget!!.id)
                        deleteTarget = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add account")
            }
        }
    ) { innerPadding ->
        if (state.accounts.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No accounts yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Add your bank accounts and cards to auto-link transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Accounts first, then whatever could not be placed. The unassigned bucket is last
                // because it is a prompt to fix something, not an account.
                items(
                    state.balances.filterNot { it.isUnassigned },
                    key = { it.account?.id ?: 0L }
                ) { balance ->
                    val account = balance.account ?: return@items
                    AccountRow(
                        balance = balance,
                        onClick = { onEditClick(account.id) },
                        onDelete = { deleteTarget = account },
                        onUseStatedBalance = { viewModel.useStatedBalance(account.id) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                state.balances.firstOrNull { it.isUnassigned }?.let { unassigned ->
                    item { UnassignedRow(unassigned) }
                }
            }
        }
    }
}

@Composable
private fun AccountRow(
    balance: AccountBalance,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onUseStatedBalance: () -> Unit
) {
    val account = balance.account ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Bank chip — the bank's own colour and monogram where the bank is recognised, so the same
        // bank looks the same on every card. A colour the owner set by hand still wins; the old
        // behaviour (a colour hashed from the account's last four digits) is only the last resort.
        val bank = BankRegistry.resolve(account.displayName)
        // The bank's own colour wins over the stored one. `colorArgb` sounds like a user choice but
        // is assigned automatically when an account is auto-created, by hashing its last four
        // digits — which is why one bank could show up in three different colours across cards.
        // Recognising the bank is the only way to make the same bank look the same everywhere; the
        // stored colour still covers accounts no bank matches.
        val chipColor = bank?.color
            ?: account.colorArgb?.toColorHex()?.toComposeColor()
            ?: "#9E9E9E".toComposeColor()
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(chipColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bank?.monogram ?: BankRegistry.monogramFor(account.displayName),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = account.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = buildAccountSubtitle(account),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (balance.isKnown) {
                Text(
                    // A card's balance is a debt, so it reads as an amount outstanding rather than
                    // as a negative number the owner has to mentally flip.
                    text = if (balance.isCreditCard) {
                        "Outstanding ${balance.outstandingInPaise.toAmountDisplay()}"
                    } else {
                        balance.balanceInPaise.toAmountDisplay()
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (balance.balanceInPaise < 0 && !balance.isCreditCard) {
                        KaasuColors.expense
                    } else {
                        KaasuColors.ink
                    }
                )
            } else {
                // Not "₹0" — that would be a confident claim about money Kaasu knows nothing about.
                Text(
                    text = "Set opening balance",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = KaasuColors.forest
                )
            }

            // The bank already told us what it thinks. Showing the disagreement is the entire
            // reconciliation feature; one tap accepts the bank's number and re-anchors.
            val drift = balance.driftInPaise
            if (drift != null && drift != 0L) {
                val stated = account.lastStatedBalanceInPaise ?: 0L
                Text(
                    text = "Your bank said ${stated.toAmountDisplay()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Use bank's figure",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = KaasuColors.forest,
                    modifier = Modifier.clickable(onClick = onUseStatedBalance)
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.DeleteOutline,
                contentDescription = "Delete ${account.displayName}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Transactions Kaasu captured but could not attribute to any account.
 *
 * Usually a message with no account tail, or the same last four digits at two banks. Without a row
 * of its own this money silently disappears from the account view, which reads as having less than
 * you do.
 */
@Composable
private fun UnassignedRow(balance: AccountBalance) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = "Not linked to an account",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "${balance.transactionCount} transaction${if (balance.transactionCount == 1) "" else "s"} " +
                "· ${balance.balanceInPaise.toAmountDisplay()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Open one to pick its account, or add the account's last 4 digits so Kaasu can link it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun buildAccountSubtitle(account: Account): String {
    val typeLabel = account.accountType.displayLabel()
    return if (account.lastFourDigits != null) "$typeLabel  •••• ${account.lastFourDigits}"
    else typeLabel
}

private fun AccountType.displayLabel(): String = when (this) {
    AccountType.SAVINGS -> "Savings Account"
    AccountType.CURRENT -> "Current Account"
    AccountType.CREDIT_CARD -> "Credit Card"
    AccountType.WALLET -> "Wallet"
    AccountType.PREPAID -> "Prepaid Card"
}
