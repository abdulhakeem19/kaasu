package com.kaasu.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_sources",
    indices = [Index("packageName", unique = true)]
)
data class AppSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true,
    val isKnownFinanceApp: Boolean = false,
    val lastSeenAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    // Accessibility screen-scrape health signal (accessibility-capture channel only — see
    // AppSourceDao.updateLastAccessibilityScrapeAttempt/Success). Null for every source that
    // doesn't have a registered ScreenScraper, and null for a scraper-backed source that has never
    // fired an event yet. Nullable with defaults so every other AppSourceEntity(...) call site
    // (DatabaseSeeder, BankSourcesViewModel.addAppSource, tests) needed no changes.
    val lastAccessibilityScrapeAttemptAt: Long? = null,
    val lastAccessibilityScrapeSuccessAt: Long? = null
)
