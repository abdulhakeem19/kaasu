package com.kaasu.app.feature.settings

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
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaasu.app.ui.theme.KaasuColors
import com.kaasu.app.core.security.BiometricAuthenticator

/**
 * The unlock gate shown over the app when app lock is enabled and the app is locked.
 * Auto-prompts biometric (if enabled & available); always offers a PIN fallback.
 */
@Composable
fun AppLockScreen(
    onUnlock: () -> Unit,
    viewModel: AppLockViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()

    var pinMode by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    val canBiometric = activity != null && BiometricAuthenticator.canAuthenticate(activity)

    fun promptBiometric() {
        if (activity != null && canBiometric) {
            BiometricAuthenticator.authenticate(
                activity,
                onSuccess = onUnlock,
                onError = { pinMode = true } // fall back to PIN on cancel/error
            )
        } else {
            pinMode = true
        }
    }

    // On first show, try biometric automatically; otherwise go straight to PIN.
    LaunchedEffect(Unit) {
        if (biometricEnabled && canBiometric) promptBiometric() else pinMode = true
    }

    Box(modifier = Modifier.fillMaxSize().background(KaasuColors.forest)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(KaasuColors.onForest),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("K", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = KaasuColors.forest)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Kaasu", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = KaasuColors.onForest)
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(KaasuColors.onForest.copy(alpha = 0.15f)).padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Locked", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = KaasuColors.onForest)
                }
            }

            Spacer(Modifier.weight(1f))

            Text("WELCOME BACK", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = KaasuColors.onForest.copy(alpha = 0.6f))
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Unlock to see\nyour money",
                fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = KaasuColors.onForest,
                lineHeight = 38.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(36.dp))

            if (!pinMode) {
                // Biometric mode — tap the ring to (re)prompt
                Box(
                    modifier = Modifier.size(120.dp).clip(CircleShape)
                        .background(KaasuColors.onForest.copy(alpha = 0.07f))
                        .border(1.dp, KaasuColors.onForest.copy(alpha = 0.2f), CircleShape)
                        .clickable { promptBiometric() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = "Fingerprint", tint = KaasuColors.onForest.copy(alpha = 0.8f), modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text("Touch the sensor", fontSize = 14.sp, color = KaasuColors.onForest.copy(alpha = 0.7f))
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { pinMode = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KaasuColors.onForest.copy(alpha = 0.15f), contentColor = KaasuColors.onForest),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Use PIN instead", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                // PIN entry mode
                PinDots(count = entered.length, error = error)
                Spacer(Modifier.height(8.dp))
                if (error) {
                    Text("Wrong PIN, try again", fontSize = 13.sp, color = KaasuColors.onForest.copy(alpha = 0.8f))
                } else {
                    Text("Enter your PIN", fontSize = 13.sp, color = KaasuColors.onForest.copy(alpha = 0.7f))
                }
                Spacer(Modifier.height(20.dp))
                PinKeypad(
                    onDigit = { d ->
                        if (entered.length < 4) {
                            error = false
                            entered += d
                            if (entered.length == 4) {
                                val attempt = entered
                                viewModel.verifyPin(attempt) { ok ->
                                    if (ok) onUnlock() else { error = true; entered = "" }
                                }
                            }
                        }
                    },
                    onBackspace = { if (entered.isNotEmpty()) entered = entered.dropLast(1) },
                    showBiometric = canBiometric && biometricEnabled,
                    onBiometric = { promptBiometric() }
                )
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))
            Text("Your data stays on this device.", fontSize = 12.sp, color = KaasuColors.onForest.copy(alpha = 0.5f))
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PinDots(count: Int, error: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(4) { i ->
            val filled = i < count
            Box(
                modifier = Modifier.size(16.dp).clip(CircleShape)
                    .background(
                        if (filled) (if (error) androidx.compose.ui.graphics.Color(0xFFFFB3B3) else KaasuColors.onForest)
                        else KaasuColors.onForest.copy(alpha = 0.2f)
                    )
            )
        }
    }
}

@Composable
private fun PinKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    showBiometric: Boolean,
    onBiometric: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(if (showBiometric) "bio" else "", "0", "del")
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
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
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .then(if (key == "del" || key == "bio") Modifier else Modifier.background(KaasuColors.onForest.copy(alpha = 0.08f)))
                                .clickable {
                                    when (key) {
                                        "del" -> onBackspace()
                                        "bio" -> onBiometric()
                                        else -> onDigit(key)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when (key) {
                                "del" -> Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete", tint = KaasuColors.onForest, modifier = Modifier.size(26.dp))
                                "bio" -> Icon(Icons.Default.Fingerprint, contentDescription = "Biometric", tint = KaasuColors.onForest, modifier = Modifier.size(30.dp))
                                else -> Text(key, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = KaasuColors.onForest)
                            }
                        }
                    }
                }
            }
        }
    }
}
