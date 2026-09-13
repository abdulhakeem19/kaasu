package com.kaasu.app.feature.settings

import com.kaasu.app.ui.theme.KaasuColors
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AppLockSetupScreen(
    onBack: () -> Unit,
    viewModel: AppLockViewModel = hiltViewModel()
) {
    val enabled by viewModel.appLockEnabled.collectAsStateWithLifecycle()
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()

    // settingPin: null = not setting; otherwise the first entry awaiting confirmation ("" = entering first)
    var settingPin by remember { mutableStateOf(false) }
    var firstEntry by remember { mutableStateOf<String?>(null) }
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun resetPinFlow() { settingPin = false; firstEntry = null; entered = ""; error = null }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 20.dp),
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
            Text("App lock", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        }

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            if (settingPin) {
                val title = if (firstEntry == null) "Choose a 4-digit PIN" else "Confirm your PIN"
                Spacer(Modifier.height(24.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(28.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    SetupPinDots(entered.length)
                }
                Box(Modifier.fillMaxWidth().height(32.dp).padding(top = 10.dp), contentAlignment = Alignment.Center) {
                    error?.let { Text(it, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = KaasuColors.forest) }
                }
                Spacer(Modifier.height(24.dp))
                SetupKeypad(
                    onDigit = { d ->
                        if (entered.length < 4) {
                            error = null
                            entered += d
                            if (entered.length == 4) {
                                val current = entered
                                if (firstEntry == null) {
                                    firstEntry = current; entered = ""
                                } else if (firstEntry == current) {
                                    viewModel.enableWithPin(current) { resetPinFlow() }
                                } else {
                                    error = "PINs didn't match, start again"; firstEntry = null; entered = ""
                                }
                            }
                        }
                    },
                    onBackspace = { if (entered.isNotEmpty()) entered = entered.dropLast(1) }
                )
                Spacer(Modifier.height(20.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable { resetPinFlow() }.padding(8.dp))
                }
            } else if (!enabled) {
                Text(
                    "Lock Kaasu behind your fingerprint or a 4-digit PIN so only you can see your money.",
                    fontSize = 14.sp, lineHeight = 21.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                PrimaryButton("Set up app lock") { settingPin = true; firstEntry = null; entered = ""; error = null }
            } else {
                StatusCard("App lock is on", "Kaasu asks for your fingerprint or PIN each time you open it.")
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Use fingerprint / face", modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { viewModel.setBiometricEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = KaasuColors.onForest, checkedTrackColor = KaasuColors.forest, uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant, uncheckedTrackColor = MaterialTheme.colorScheme.outline)
                    )
                }
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Change PIN") { settingPin = true; firstEntry = null; entered = ""; error = null }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Turn off app lock", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, modifier = Modifier.clickable { viewModel.disable() }.padding(12.dp))
                }
            }
        }
    }
}

@Composable
private fun StatusCard(title: String, body: String) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(KaasuColors.forest).padding(20.dp)
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = KaasuColors.onForest)
        Spacer(Modifier.height(6.dp))
        Text(body, fontSize = 13.sp, lineHeight = 19.sp, color = KaasuColors.onForest.copy(alpha = 0.8f))
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(KaasuColors.forest).clickable(onClick = onClick).padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = KaasuColors.onForest)
    }
}

@Composable
private fun SetupPinDots(count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        repeat(4) { i ->
            val filled = i < count
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (filled) KaasuColors.forest else MaterialTheme.colorScheme.surface)
                    .border(
                        1.5.dp,
                        if (filled) KaasuColors.forest else MaterialTheme.colorScheme.outline,
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun SetupKeypad(onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "del"))
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Box(modifier = Modifier.size(76.dp))
                    } else {
                        val isDel = key == "del"
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(if (isDel) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface)
                                .then(if (isDel) Modifier else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
                                .clickable { if (isDel) onBackspace() else onDigit(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDel) {
                                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(26.dp))
                            } else {
                                Text(key, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}
