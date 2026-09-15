package com.kaasu.app.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Teaches the database that a transfer has two ends.
 *
 * Until now the only record that two rows were halves of one movement was an arrow inside
 * `merchantName` ("IDFC FIRST → Union Bank"), which no query could use. So a credit-card bill paid
 * from savings counted the money twice: once when the card was charged, once when the bill was
 * paid.
 *
 * The pair is modelled as a *group* rather than one row or a `linkedTransactionId`. Capture
 * genuinely produces two rows — each bank announces its own leg with its own rawText, and
 * DuplicateChecker keys on that, so collapsing them into a single row would make a redelivered SMS
 * look new. A group id also degrades gracefully: delete one leg and the survivor still reads
 * "OUT of Savings".
 *
 * `counterpartAccountId` is for display and repair only and is never summed. Each row moves only
 * its own account, so a complete group nets to zero across all accounts.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `transactions` ADD COLUMN `transferGroupId` TEXT")
        database.execSQL("ALTER TABLE `transactions` ADD COLUMN `transferRole` TEXT")
        database.execSQL("ALTER TABLE `transactions` ADD COLUMN `counterpartAccountId` INTEGER")
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transactions_transferGroupId` " +
                "ON `transactions` (`transferGroupId`)"
        )

        // The double-count was also seeded as data: the stock "credit card" and "card bill" rules
        // filed bill payments into the Bills category, which made them spend by definition. They
        // become type-override rules instead. The Bills budget will shrink for existing installs —
        // correct, since those purchases are already categorised individually on the card.
        database.execSQL(
            """UPDATE `rules`
               SET `transactionType` = 'TRANSFER', `categoryId` = NULL, `priority` = 20
               WHERE `isSystem` = 1 AND `matchText` IN ('credit card', 'card bill')"""
        )
    }
}
