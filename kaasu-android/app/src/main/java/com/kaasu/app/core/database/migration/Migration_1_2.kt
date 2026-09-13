package com.kaasu.app.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `accounts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `displayName` TEXT NOT NULL,
                `lastFourDigits` TEXT,
                `accountType` TEXT NOT NULL,
                `colorArgb` INTEGER,
                `isActive` INTEGER NOT NULL DEFAULT 1,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_lastFourDigits` ON `accounts` (`lastFourDigits`)")
        database.execSQL("ALTER TABLE `transactions` ADD COLUMN `accountId` INTEGER")
    }
}
