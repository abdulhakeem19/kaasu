package com.kaasu.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.core.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import com.kaasu.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    // null = still reading from DataStore (one frame); true/false drives splash gate routing
    val isOnboardingComplete = settingsDataStore.isOnboardingComplete
        .map { it as Boolean? }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    val appTheme = settingsDataStore.appTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = SettingsDataStore.Theme.SYSTEM)

    val appLockEnabled = settingsDataStore.appLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = false)

    // In-memory unlock state. Starts locked; the gate shows whenever appLockEnabled && locked.
    private val _locked = MutableStateFlow(true)
    val locked = _locked.asStateFlow()

    fun unlock() { _locked.value = false }

    // Called when the app goes to background so it re-locks on return.
    fun lock() { _locked.value = true }

    // What's-new: show once per app version (after onboarding is complete).
    private val _showWhatsNew = MutableStateFlow(false)
    val showWhatsNew = _showWhatsNew.asStateFlow()

    init {
        viewModelScope.launch {
            if (settingsDataStore.isOnboardingComplete.first() &&
                settingsDataStore.lastSeenVersionCode() < BuildConfig.VERSION_CODE
            ) {
                _showWhatsNew.value = true
            }
        }
        // Keep the global currency symbol used by formatRupees() in sync.
        viewModelScope.launch {
            settingsDataStore.currencySymbol.collect { com.kaasu.app.core.util.CurrencySymbol.value = it }
        }
    }

    fun dismissWhatsNew() {
        _showWhatsNew.value = false
        viewModelScope.launch { settingsDataStore.setLastSeenVersionCode(BuildConfig.VERSION_CODE) }
    }
}
