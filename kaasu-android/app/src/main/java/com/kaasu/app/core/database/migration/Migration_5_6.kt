package com.kaasu.app.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Adds the isDuplicate flag backing the manual "mark as duplicate" feature.
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE `transactions` ADD COLUMN `isDuplicate` INTEGER NOT NULL DEFAULT 0"
        )
    }
}
