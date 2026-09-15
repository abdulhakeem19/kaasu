package com.kaasu.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kaasu.app.core.database.dao.AccountDao
import com.kaasu.app.core.database.dao.AppSourceDao
import com.kaasu.app.core.database.dao.CategoryDao
import com.kaasu.app.core.database.dao.IgnoredPatternDao
import com.kaasu.app.core.database.dao.MerchantAliasDao
import com.kaasu.app.core.database.dao.MonthlyBudgetDao
import com.kaasu.app.core.database.dao.RuleDao
import com.kaasu.app.core.database.dao.SmsSenderDao
import com.kaasu.app.core.database.dao.TransactionDao
import com.kaasu.app.core.database.entity.AccountEntity
import com.kaasu.app.core.database.entity.AppSourceEntity
import com.kaasu.app.core.database.entity.CategoryEntity
import com.kaasu.app.core.database.entity.IgnoredPatternEntity
import com.kaasu.app.core.database.entity.MerchantAliasEntity
import com.kaasu.app.core.database.entity.MonthlyBudgetEntity
import com.kaasu.app.core.database.entity.RuleEntity
import com.kaasu.app.core.database.entity.SmsSenderEntity
import com.kaasu.app.core.database.entity.TransactionEntity
import com.kaasu.app.core.database.migration.MIGRATION_1_2
import com.kaasu.app.core.database.migration.MIGRATION_2_3
import com.kaasu.app.core.database.migration.MIGRATION_3_4
import com.kaasu.app.core.database.migration.MIGRATION_4_5
import com.kaasu.app.core.database.migration.MIGRATION_5_6
import com.kaasu.app.core.database.migration.MIGRATION_6_7
import com.kaasu.app.core.database.migration.MIGRATION_7_8
import com.kaasu.app.core.database.migration.MIGRATION_8_9

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        RuleEntity::class,
        AppSourceEntity::class,
        MonthlyBudgetEntity::class,
        IgnoredPatternEntity::class,
        AccountEntity::class,
        MerchantAliasEntity::class,
        SmsSenderEntity::class,
    ],
    version = 9,
    exportSchema = true
)
abstract class KaasuDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun ruleDao(): RuleDao
    abstract fun appSourceDao(): AppSourceDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao
    abstract fun ignoredPatternDao(): IgnoredPatternDao
    abstract fun accountDao(): AccountDao
    abstract fun merchantAliasDao(): MerchantAliasDao
    abstract fun smsSenderDao(): SmsSenderDao

    companion object {
        const val NAME = "kaasu.db"
        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
            MIGRATION_7_8, MIGRATION_8_9
        )
    }
}
