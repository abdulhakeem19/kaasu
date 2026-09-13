package com.kaasu.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kaasu.app.core.database.entity.AppSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSourceDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(appSource: AppSourceEntity): Long

    @Update
    suspend fun update(appSource: AppSourceEntity)

    @Query("SELECT * FROM app_sources ORDER BY appName ASC")
    fun getAll(): Flow<List<AppSourceEntity>>

    @Query("SELECT * FROM app_sources WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): AppSourceEntity?

    @Query("SELECT * FROM app_sources WHERE isKnownFinanceApp = 1 AND isEnabled = 1")
    suspend fun getKnownFinanceApps(): List<AppSourceEntity>

    @Query("UPDATE app_sources SET lastSeenAt = :time, updatedAt = :time WHERE packageName = :packageName")
    suspend fun updateLastSeen(packageName: String, time: Long)

    // Accessibility screen-scrape health signal — called from KaasuAccessibilityService on every
    // handled event (attempt) and again only when at least one candidate was actually inserted
    // (success), so Settings can distinguish "never attempted" / "attempted, nothing new" / "last
    // successful read <time>" for a scraper-backed source (see BankSourcesScreen).
    @Query("UPDATE app_sources SET lastAccessibilityScrapeAttemptAt = :time, updatedAt = :time WHERE packageName = :packageName")
    suspend fun updateLastAccessibilityScrapeAttempt(packageName: String, time: Long)

    @Query("UPDATE app_sources SET lastAccessibilityScrapeSuccessAt = :time, updatedAt = :time WHERE packageName = :packageName")
    suspend fun updateLastAccessibilityScrapeSuccess(packageName: String, time: Long)
}
