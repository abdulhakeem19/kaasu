package com.kaasu.app.data.repository

import com.kaasu.app.core.database.dao.AccountDao
import com.kaasu.app.data.mapper.toDomain
import com.kaasu.app.data.mapper.toEntity
import com.kaasu.app.domain.model.Account
import com.kaasu.app.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val dao: AccountDao
) : AccountRepository {

    override fun getAll(): Flow<List<Account>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Account? =
        dao.getById(id)?.toDomain()

    override suspend fun getByLastFour(lastFour: String): List<Account> =
        dao.getByLastFour(lastFour).map { it.toDomain() }

    override suspend fun insert(account: Account): Long =
        dao.insert(account.toEntity())

    override suspend fun update(account: Account) =
        dao.update(account.toEntity())

    /**
     * Soft delete. Transactions keep pointing at this account, so their money stays visible and
     * countable — a hard delete would strand them against a missing id and make the money vanish.
     */
    override suspend fun delete(id: Long) =
        dao.deactivate(id)

    override suspend fun setLastStatedBalance(id: Long, balanceInPaise: Long, at: Long) =
        dao.setLastStatedBalance(id, balanceInPaise, at)
}
