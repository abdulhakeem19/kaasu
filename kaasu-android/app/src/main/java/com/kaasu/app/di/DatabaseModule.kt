package com.kaasu.app.di

import android.content.Context
import androidx.room.Room
import com.kaasu.app.core.database.KaasuDatabase
import com.kaasu.app.core.database.dao.AccountDao
import com.kaasu.app.core.database.dao.AppSourceDao
import com.kaasu.app.core.database.dao.CategoryDao
import com.kaasu.app.core.database.dao.IgnoredPatternDao
import com.kaasu.app.core.database.dao.MerchantAliasDao
import com.kaasu.app.core.database.dao.MonthlyBudgetDao
import com.kaasu.app.core.database.dao.RuleDao
import com.kaasu.app.core.database.dao.SmsSenderDao
import com.kaasu.app.core.database.dao.TransactionDao
import com.kaasu.app.core.database.seed.DatabaseSeeder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KaasuDatabase =
        Room.databaseBuilder(context, KaasuDatabase::class.java, KaasuDatabase.NAME)
            .addMigrations(*KaasuDatabase.ALL_MIGRATIONS)
            .addCallback(DatabaseSeeder)
            .build()

    @Provides fun provideTransactionDao(db: KaasuDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideCategoryDao(db: KaasuDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideRuleDao(db: KaasuDatabase): RuleDao = db.ruleDao()
    @Provides fun provideAppSourceDao(db: KaasuDatabase): AppSourceDao = db.appSourceDao()
    @Provides fun provideMonthlyBudgetDao(db: KaasuDatabase): MonthlyBudgetDao = db.monthlyBudgetDao()
    @Provides fun provideIgnoredPatternDao(db: KaasuDatabase): IgnoredPatternDao = db.ignoredPatternDao()
    @Provides fun provideAccountDao(db: KaasuDatabase): AccountDao = db.accountDao()
    @Provides fun provideMerchantAliasDao(db: KaasuDatabase): MerchantAliasDao = db.merchantAliasDao()
    @Provides fun provideSmsSenderDao(db: KaasuDatabase): SmsSenderDao = db.smsSenderDao()
}
