package com.kaasu.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.accessibility.scraper.ScraperRegistry
import com.kaasu.app.core.database.dao.AppSourceDao
import com.kaasu.app.core.database.entity.AppSourceEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BankSourcesViewModel @Inject constructor(
    private val appSourceDao: AppSourceDao,
    private val scraperRegistry: ScraperRegistry,
) : ViewModel() {

    val appSources = appSourceDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Packages with a registered accessibility ScreenScraper — BankSourcesScreen shows the extra
    // "Screen reading" health-signal line only for these (today: GPay, PhonePe).
    val accessibilityScrapedPackages: Set<String> = scraperRegistry.supportedPackages

    fun toggleAppSource(entity: AppSourceEntity, enabled: Boolean) {
        viewModelScope.launch { appSourceDao.update(entity.copy(isEnabled = enabled)) }
    }

    fun addAppSource(packageName: String, appName: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            appSourceDao.insert(
                AppSourceEntity(
                    packageName = packageName,
                    appName = appName,
                    isEnabled = true,
                    isKnownFinanceApp = true,
                    lastSeenAt = null,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }
}
