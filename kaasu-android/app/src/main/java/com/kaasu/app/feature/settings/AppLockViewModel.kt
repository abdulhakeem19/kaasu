package com.kaasu.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.core.datastore.SettingsDataStore
import com.kaasu.app.core.security.PinHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val settings: SettingsDataStore
) : ViewModel() {

    val appLockEnabled = settings.appLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val biometricEnabled = settings.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Enable app lock with a freshly chosen PIN. */
    fun enableWithPin(pin: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val salt = PinHasher.newSalt()
            settings.setAppLock(enabled = true, pinHash = PinHasher.hash(pin, salt), pinSalt = salt)
            onDone()
        }
    }

    fun disable(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            settings.setAppLock(enabled = false, pinHash = null, pinSalt = null)
            onDone()
        }
    }

    fun setBiometricEnabled(value: Boolean) {
        viewModelScope.launch { settings.setBiometricEnabled(value) }
    }

    /** Verify a PIN entered at the lock gate. */
    fun verifyPin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val hash = settings.pinHash()
            val salt = settings.pinSalt()
            onResult(hash != null && salt != null && PinHasher.verify(pin, salt, hash))
        }
    }
}
