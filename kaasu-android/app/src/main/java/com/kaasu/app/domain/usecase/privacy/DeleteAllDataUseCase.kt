package com.kaasu.app.domain.usecase.privacy

import com.kaasu.app.core.database.dao.RuleDao
import com.kaasu.app.core.datastore.SettingsDataStore
import com.kaasu.app.domain.repository.CategoryRepository
import com.kaasu.app.domain.repository.TransactionRepository
import javax.inject.Inject

// Called by PrivacyDataViewModel — hard deletes all user data per the privacy policy
// Keeps: seeded categories (isDefault=1) and system rules (priority=10)
// Clears: all transactions, user-created categories, user rules, monthly budget
class DeleteAllDataUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val ruleDao: RuleDao,
    private val settingsDataStore: SettingsDataStore,
) {
    suspend operator fun invoke() {
        transactionRepository.deleteAll()
        categoryRepository.deleteAllUserCreated()
        ruleDao.deleteUserRules()
        settingsDataStore.setMonthlyBudget(0)
    }
}
