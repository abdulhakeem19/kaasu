package com.kaasu.app.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Adds the sms_senders table backing SmsFilter's per-sender enable/disable registry (SMS capture).
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sms_senders` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `senderId` TEXT NOT NULL,
                `displayLabel` TEXT,
                `isEnabled` INTEGER NOT NULL DEFAULT 1,
                `isKnownFinanceSender` INTEGER NOT NULL DEFAULT 0,
                `lastSeenAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_sms_senders_senderId` ON `sms_senders` (`senderId`)"
        )
    }
}
