package com.kaasu.app.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Adds the accessibility-scrape health-signal columns to app_sources (accessibility-capture work).
// This channel fails silently by design otherwise — these two nullable timestamps are what Settings
// surfaces ("last successful GPay read 2 days ago") so the owner can tell whether it's working at all.
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE `app_sources` ADD COLUMN `lastAccessibilityScrapeAttemptAt` INTEGER"
        )
        database.execSQL(
            "ALTER TABLE `app_sources` ADD COLUMN `lastAccessibilityScrapeSuccessAt` INTEGER"
        )
    }
}
