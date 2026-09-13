package com.kaasu.app.di

import com.kaasu.app.data.repository.AccountRepositoryImpl
import com.kaasu.app.data.repository.CategoryRepositoryImpl
import com.kaasu.app.data.repository.MerchantAliasRepositoryImpl
import com.kaasu.app.data.repository.RuleRepositoryImpl
import com.kaasu.app.data.repository.TransactionRepositoryImpl
import com.kaasu.app.domain.repository.AccountRepository
import com.kaasu.app.domain.repository.CategoryRepository
import com.kaasu.app.domain.repository.MerchantAliasRepository
import com.kaasu.app.domain.repository.RuleRepository
import com.kaasu.app.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds @Singleton
    abstract fun bindRuleRepository(impl: RuleRepositoryImpl): RuleRepository

    @Binds @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds @Singleton
    abstract fun bindMerchantAliasRepository(impl: MerchantAliasRepositoryImpl): MerchantAliasRepository
}
