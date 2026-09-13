package com.kaasu.app.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Adds parentId, linking a split slice back to the original (now isIgnored) parent transaction.
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `transactions` ADD COLUMN `parentId` INTEGER")
    }
}
