package com.kaasu.app.feature.profile

import com.kaasu.app.ui.theme.KaasuColors
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Local editable name; committed on change (debounced by simply saving each edit).
    var name by remember(state.displayName) { mutableStateOf(state.displayName) }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text("Profile", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        }

        // Avatar preview
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(KaasuColors.forest),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.trim().firstOrNull()?.uppercase() ?: "K",
                    fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = KaasuColors.onForest
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.size(4.dp))
            // Name
            FieldLabel("Your name")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; viewModel.setName(it) },
                placeholder = { Text("e.g. Aarav") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Shown on your dashboard greeting.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Currency
            FieldLabel("Currency symbol")
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("₹", "$", "€", "£", "¥", "₨", "৳").forEach { sym ->
                    val sel = sym == state.currencySymbol
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            .then(if (sel) Modifier.background(KaasuColors.forest) else Modifier.background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)))
                            .clickable { viewModel.setCurrency(sym) }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text(sym, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (sel) KaasuColors.onForest else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Budget cycle start
            FieldLabel("Budget cycle starts on")
            var dayText by remember(state.monthStartDay) { mutableStateOf(state.monthStartDay.toString()) }
            OutlinedTextField(
                value = dayText,
                onValueChange = { v ->
                    val digits = v.filter(Char::isDigit).take(2)
                    dayText = digits
                    digits.toIntOrNull()?.let { if (it in 1..28) viewModel.setMonthStartDay(it) }
                },
                label = { Text("Day of month (1–28)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Budgets and monthly totals run from this day each month — handy if you're paid mid-month.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(24.dp))
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
