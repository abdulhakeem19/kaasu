package com.kaasu.app.domain.repository

import com.kaasu.app.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAll(): Flow<List<Account>>
    suspend fun getById(id: Long): Account?
    // Returns all active accounts whose last-four digits match. Multiple results = ambiguous (different banks, same digits).
    suspend fun getByLastFour(lastFour: String): List<Account>
    suspend fun insert(account: Account): Long
    suspend fun update(account: Account)
    suspend fun delete(id: Long)
}
