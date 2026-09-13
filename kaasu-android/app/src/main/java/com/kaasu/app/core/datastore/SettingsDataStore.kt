package com.kaasu.app.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val isOnboardingComplete: Flow<Boolean> =
        dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }

    val isPermissionExplanationShown: Flow<Boolean> =
        dataStore.data.map { it[Keys.PERMISSION_EXPLANATION_SHOWN] ?: false }

    val monthlyBudgetInPaise: Flow<Long> =
        dataStore.data.map { it[Keys.MONTHLY_BUDGET_IN_PAISE] ?: 0L }

    val appTheme: Flow<String> =
        dataStore.data.map { it[Keys.APP_THEME] ?: Theme.SYSTEM }

    // ── Personalization ──────────────────────────────────────────────────────
    val displayName: Flow<String> =
        dataStore.data.map { it[Keys.DISPLAY_NAME] ?: "" }

    val currencySymbol: Flow<String> =
        dataStore.data.map { it[Keys.CURRENCY_SYMBOL] ?: "₹" }

    // Day of month the budget cycle starts (1–28). Default 1.
    val monthStartDay: Flow<Int> =
        dataStore.data.map { (it[Keys.MONTH_START_DAY] ?: 1).coerceIn(1, 28) }

    suspend fun setDisplayName(name: String) {
        dataStore.edit { it[Keys.DISPLAY_NAME] = name.trim() }
    }

    suspend fun setCurrencySymbol(symbol: String) {
        dataStore.edit { it[Keys.CURRENCY_SYMBOL] = symbol.trim().ifBlank { "₹" } }
    }

    suspend fun setMonthStartDay(day: Int) {
        dataStore.edit { it[Keys.MONTH_START_DAY] = day.coerceIn(1, 28) }
    }

    val hideAmountsOnLock: Flow<Boolean> =
        dataStore.data.map { it[Keys.HIDE_AMOUNTS_ON_LOCK] ?: true }

    val appLockEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.APP_LOCK_ENABLED] ?: false }

    // Biometric is opt-in: a PIN is always set first, fingerprint/face is enabled explicitly.
    val biometricEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.BIOMETRIC_ENABLED] ?: false }

    // Suspend reads for the lock gate / verification (no need to observe)
    suspend fun isAppLockEnabledOnce(): Boolean =
        dataStore.data.map { it[Keys.APP_LOCK_ENABLED] ?: false }.first()

    suspend fun pinHash(): String? =
        dataStore.data.map { it[Keys.PIN_HASH] }.first()

    suspend fun pinSalt(): String? =
        dataStore.data.map { it[Keys.PIN_SALT] }.first()

    suspend fun setAppLock(enabled: Boolean, pinHash: String?, pinSalt: String?) {
        dataStore.edit {
            it[Keys.APP_LOCK_ENABLED] = enabled
            if (pinHash != null) it[Keys.PIN_HASH] = pinHash
            if (pinSalt != null) it[Keys.PIN_SALT] = pinSalt
            if (!enabled) {
                it.remove(Keys.PIN_HASH)
                it.remove(Keys.PIN_SALT)
            }
        }
    }

    suspend fun setBiometricEnabled(value: Boolean) {
        dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = value }
    }

    // Last app version code the user has seen the "What's new" sheet for (0 = never).
    suspend fun lastSeenVersionCode(): Int =
        dataStore.data.map { it[Keys.LAST_SEEN_VERSION] ?: 0 }.first()

    suspend fun setLastSeenVersionCode(code: Int) {
        dataStore.edit { it[Keys.LAST_SEEN_VERSION] = code }
    }

    val budgetAlertsEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.BUDGET_ALERTS] ?: false }

    val subscriptionRenewalsEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.SUBSCRIPTION_RENEWALS] ?: false }

    val dailyNudgeEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.DAILY_NUDGE] ?: false }

    val weeklySummaryEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.WEEKLY_SUMMARY] ?: false }

    suspend fun setHideAmountsOnLock(value: Boolean) {
        dataStore.edit { it[Keys.HIDE_AMOUNTS_ON_LOCK] = value }
    }

    suspend fun setBudgetAlertsEnabled(value: Boolean) {
        dataStore.edit { it[Keys.BUDGET_ALERTS] = value }
    }

    suspend fun setSubscriptionRenewalsEnabled(value: Boolean) {
        dataStore.edit { it[Keys.SUBSCRIPTION_RENEWALS] = value }
    }

    suspend fun setDailyNudgeEnabled(value: Boolean) {
        dataStore.edit { it[Keys.DAILY_NUDGE] = value }
    }

    suspend fun setWeeklySummaryEnabled(value: Boolean) {
        dataStore.edit { it[Keys.WEEKLY_SUMMARY] = value }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setPermissionExplanationShown(shown: Boolean) {
        dataStore.edit { it[Keys.PERMISSION_EXPLANATION_SHOWN] = shown }
    }

    suspend fun setMonthlyBudget(amountInPaise: Long) {
        dataStore.edit { it[Keys.MONTHLY_BUDGET_IN_PAISE] = amountInPaise }
    }

    suspend fun setAppTheme(theme: String) {
        dataStore.edit { it[Keys.APP_THEME] = theme }
    }

    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val PERMISSION_EXPLANATION_SHOWN = booleanPreferencesKey("permission_explanation_shown")
        val MONTHLY_BUDGET_IN_PAISE = longPreferencesKey("monthly_budget_in_paise")
        val APP_THEME = stringPreferencesKey("app_theme")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val MONTH_START_DAY = intPreferencesKey("month_start_day")
        val HIDE_AMOUNTS_ON_LOCK = booleanPreferencesKey("hide_amounts_on_lock")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val LAST_SEEN_VERSION = intPreferencesKey("last_seen_version")
        val BUDGET_ALERTS = booleanPreferencesKey("budget_alerts")
        val SUBSCRIPTION_RENEWALS = booleanPreferencesKey("subscription_renewals")
        val DAILY_NUDGE = booleanPreferencesKey("daily_nudge")
        val WEEKLY_SUMMARY = booleanPreferencesKey("weekly_summary")
    }

    object Theme {
        const val SYSTEM = "system"
        const val LIGHT = "light"
        const val DARK = "dark"
    }
}
