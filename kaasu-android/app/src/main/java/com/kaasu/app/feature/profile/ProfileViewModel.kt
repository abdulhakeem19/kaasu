package com.kaasu.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.core.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val displayName: String = "",
    val currencySymbol: String = "₹",
    val monthStartDay: Int = 1,
    val isLoading: Boolean = true
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settings: SettingsDataStore
) : ViewModel() {

    val uiState = combine(
        settings.displayName,
        settings.currencySymbol,
        settings.monthStartDay
    ) { name, currency, monthStart ->
        ProfileUiState(name, currency, monthStart, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun setName(name: String) { viewModelScope.launch { settings.setDisplayName(name) } }
    fun setCurrency(symbol: String) { viewModelScope.launch { settings.setCurrencySymbol(symbol) } }
    fun setMonthStartDay(day: Int) { viewModelScope.launch { settings.setMonthStartDay(day) } }
}
