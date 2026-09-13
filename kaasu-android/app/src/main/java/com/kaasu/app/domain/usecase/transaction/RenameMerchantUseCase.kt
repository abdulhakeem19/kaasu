package com.kaasu.app.domain.usecase.transaction

import com.kaasu.app.domain.repository.MerchantAliasRepository
import com.kaasu.app.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Renames a merchant everywhere: records an alias so future captures of [oldName] are stored as
 * [newName], and updates existing transactions that currently use [oldName].
 */
class RenameMerchantUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val merchantAliasRepository: MerchantAliasRepository,
) {
    suspend operator fun invoke(oldName: String, newName: String) {
        val from = oldName.trim()
        val to = newName.trim()
        if (from.isEmpty() || to.isEmpty() || from.equals(to, ignoreCase = true)) return
        merchantAliasRepository.setAlias(from, to)
        transactionRepository.renameMerchant(from, to)
    }
}
