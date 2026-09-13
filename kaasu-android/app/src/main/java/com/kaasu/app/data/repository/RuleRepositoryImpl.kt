package com.kaasu.app.data.repository

import com.kaasu.app.core.database.dao.RuleDao
import com.kaasu.app.core.database.entity.RuleEntity
import com.kaasu.app.domain.model.Rule
import com.kaasu.app.domain.model.MatchType
import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.domain.repository.RuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RuleRepositoryImpl @Inject constructor(
    private val dao: RuleDao
) : RuleRepository {

    override fun getAll(): Flow<List<Rule>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getActiveByPriority(): Flow<List<Rule>> =
        dao.getActiveByPriority().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Rule? =
        dao.getById(id)?.toDomain()

    override suspend fun insert(rule: Rule): Long {
        val now = System.currentTimeMillis()
        return dao.insert(rule.toEntity(createdAt = now, updatedAt = now))
    }

    override suspend fun update(rule: Rule) {
        val existing = dao.getById(rule.id) ?: return
        dao.update(rule.toEntity(createdAt = existing.createdAt, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    private fun RuleEntity.toDomain() = Rule(
        id = id,
        name = name,
        matchText = matchText,
        matchType = runCatching { MatchType.valueOf(matchType) }.getOrDefault(MatchType.CONTAINS),
        categoryId = categoryId,
        transactionType = transactionType?.let {
            runCatching { TransactionType.valueOf(it) }.getOrNull()
        },
        sourceAppPackage = sourceAppPackage,
        priority = priority,
        isSystem = isSystem,
        isActive = isActive
    )

    private fun Rule.toEntity(createdAt: Long, updatedAt: Long) = RuleEntity(
        id = id,
        name = name,
        matchText = matchText,
        matchType = matchType.name,
        categoryId = categoryId,
        transactionType = transactionType?.name,
        sourceAppPackage = sourceAppPackage,
        priority = priority,
        isSystem = isSystem,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
