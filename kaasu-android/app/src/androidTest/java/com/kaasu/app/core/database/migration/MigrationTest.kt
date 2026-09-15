package com.kaasu.app.core.database.migration

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kaasu.app.core.database.KaasuDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migrations are now load-bearing for money.
 *
 * Before transfer groups, a bad migration cost at worst some cosmetic detail. Now the columns that
 * decide whether a payment counts as spending are added by 8→9, so a migration that half-applies
 * leaves totals wrong rather than merely ugly — and a migration that throws leaves the database
 * unopenable with the owner's only copy of their history inside it.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        KaasuDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate8To9_addsTransferColumnsAndKeepsExistingRows() {
        val db = helper.createDatabase(TEST_DB, 8)
        db.execSQL(
            """INSERT INTO transactions
               (amountInPaise, currency, type, merchantName, categoryId, sourceAppPackage,
                sourceAppName, paymentMode, rawText, rawTextHash, confidenceScore,
                transactionTime, createdAt, updatedAt, isManual, isTransfer, isRefund,
                isIgnored, note, accountId, isRecurring, parentId, isDuplicate)
               VALUES (239900, 'INR', 'EXPENSE', 'CLAUDE AI', NULL, NULL, NULL, NULL, NULL,
                       'hash', 90, 1756684800000, 0, 0, 0, 0, 0, 0, NULL, NULL, 0, NULL, 0)"""
        )
        db.close()

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 9, true, MIGRATION_8_9)

        migrated.query("SELECT * FROM transactions").use { c ->
            assertEquals("the existing row survives", 1, c.count)
            assertTrue(c.moveToFirst())
            assertEquals(239900L, c.getLong(c.getColumnIndexOrThrow("amountInPaise")))
            // Old rows are not retroactively guessed at — TransferBackfillManager repairs them
            // deliberately, where a wrong guess can be seen and undone.
            assertTrue(c.isNull(c.getColumnIndexOrThrow("transferGroupId")))
            assertTrue(c.isNull(c.getColumnIndexOrThrow("transferRole")))
            assertTrue(c.isNull(c.getColumnIndexOrThrow("counterpartAccountId")))
        }
        migrated.close()
    }

    @Test
    fun migrate8To9_turnsSeededCardBillRulesIntoTypeOverrides() {
        val db = helper.createDatabase(TEST_DB, 8)
        // How an existing install looks: the double-count encoded as a seeded rule.
        db.execSQL("INSERT INTO categories (name, icon, colorHex, type, isSystem, isActive, sortOrder, createdAt, updatedAt) " +
            "VALUES ('Bills', 'receipt', '#888888', 'EXPENSE', 1, 1, 0, 0, 0)")
        db.execSQL(
            """INSERT INTO rules (name, matchText, matchType, categoryId, transactionType,
               sourceAppPackage, priority, isSystem, isActive, createdAt, updatedAt)
               SELECT 'Credit Card', 'credit card', 'CONTAINS', id, NULL, NULL, 10, 1, 1, 0, 0
               FROM categories WHERE name = 'Bills' LIMIT 1"""
        )
        db.close()

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 9, true, MIGRATION_8_9)

        migrated.query("SELECT categoryId, transactionType FROM rules WHERE matchText = 'credit card'").use { c ->
            assertTrue(c.moveToFirst())
            assertTrue("a bill payment belongs in no spending category", c.isNull(0))
            assertEquals("TRANSFER", c.getString(1))
        }
        migrated.close()
    }

    @Test
    fun migrate8To9_leavesUserRulesAlone() {
        val db = helper.createDatabase(TEST_DB, 8)
        db.execSQL(
            """INSERT INTO rules (name, matchText, matchType, categoryId, transactionType,
               sourceAppPackage, priority, isSystem, isActive, createdAt, updatedAt)
               VALUES ('My card rule', 'credit card', 'CONTAINS', 42, NULL, NULL, 100, 0, 1, 0, 0)"""
        )
        db.close()

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 9, true, MIGRATION_8_9)

        migrated.query("SELECT categoryId FROM rules WHERE isSystem = 0").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("a rule the owner wrote is theirs, not ours to rewrite", 42L, c.getLong(0))
        }
        migrated.close()
    }

    @Test
    fun migrateAll_fromTheOldestSchema() {
        helper.createDatabase(TEST_DB, 1).close()
        helper.runMigrationsAndValidate(
            TEST_DB, 9, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
            MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
        ).close()
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
