package com.kaasu.app.domain.repository

import com.kaasu.app.domain.model.Rule
import kotlinx.coroutines.flow.Flow

interface RuleRepository {
    fun getAll(): Flow<List<Rule>>
    fun getActiveByPriority(): Flow<List<Rule>>
    suspend fun getById(id: Long): Rule?
    suspend fun insert(rule: Rule): Long
    suspend fun update(rule: Rule)
    suspend fun delete(id: Long)
}
