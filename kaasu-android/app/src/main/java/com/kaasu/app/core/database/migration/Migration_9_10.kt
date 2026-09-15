package com.kaasu.app.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Gives accounts an opening balance to count forward from.
 *
 * The balance itself is deliberately **not** a column. It is derived as
 * `openingBalance + Σ signedDelta(transactions after openingBalanceAt)`, computed on read. An
 * incrementally-maintained running total is a corruption farm: edit, delete, split, import,
 * backfill and restore each have to remember to adjust it, and the first path that forgets poisons
 * it silently and permanently. A derived figure can be wrong, but only in ways that are visible and
 * that fixing the underlying rows also fixes.
 *
 * `lastStatedBalance*` is what the bank said in its own SMS. Advisory only — it is shown next to the
 * derived figure so a disagreement is visible, and re-anchoring is one tap the owner chooses to
 * take. Letting the SMS figure silently become the balance would make every missed capture
 * invisible.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // The anchor the owner enters once. Null means "not set" — distinct from a confident ₀,
        // which is a balance the app has no business claiming to know.
        database.execSQL("ALTER TABLE `accounts` ADD COLUMN `openingBalanceInPaise` INTEGER")
        database.execSQL("ALTER TABLE `accounts` ADD COLUMN `openingBalanceAt` INTEGER")

        // What the bank last stated, for reconciliation.
        database.execSQL("ALTER TABLE `accounts` ADD COLUMN `lastStatedBalanceInPaise` INTEGER")
        database.execSQL("ALTER TABLE `accounts` ADD COLUMN `lastStatedBalanceAt` INTEGER")

        // Credit cards only: asked once in Add/Edit Account, since they never change.
        database.execSQL("ALTER TABLE `accounts` ADD COLUMN `creditLimitInPaise` INTEGER")
        database.execSQL("ALTER TABLE `accounts` ADD COLUMN `statementDay` INTEGER")
        database.execSQL("ALTER TABLE `accounts` ADD COLUMN `dueDay` INTEGER")
    }
}
