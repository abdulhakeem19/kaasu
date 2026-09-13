package com.kaasu.app.data.repository

import com.kaasu.app.core.database.dao.CategoryDao
import com.kaasu.app.data.mapper.toDomain
import com.kaasu.app.data.mapper.toEntity
import com.kaasu.app.domain.model.Category
import com.kaasu.app.domain.model.CategoryType
import com.kaasu.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao
) : CategoryRepository {

    override fun getAll(): Flow<List<Category>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getAllActive(): Flow<List<Category>> =
        dao.getAllActive().map { list -> list.map { it.toDomain() } }

    override fun getByType(type: CategoryType): Flow<List<Category>> =
        dao.getByType(type.name).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Category? =
        dao.getById(id)?.toDomain()

    override suspend fun insert(category: Category): Long {
        val now = System.currentTimeMillis()
        return dao.insert(category.toEntity(createdAt = now, updatedAt = now))
    }

    override suspend fun update(category: Category) {
        val existing = dao.getById(category.id) ?: return
        dao.update(category.toEntity(createdAt = existing.createdAt, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun archive(id: Long) =
        dao.archive(id, System.currentTimeMillis())

    override suspend fun delete(id: Long) =
        dao.delete(id)

    override suspend fun deleteAllUserCreated() = dao.deleteUserCreated()
}
