package com.kaasu.app.domain.repository

import com.kaasu.app.domain.model.Category
import com.kaasu.app.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAll(): Flow<List<Category>>
    fun getAllActive(): Flow<List<Category>>
    fun getByType(type: CategoryType): Flow<List<Category>>
    suspend fun getById(id: Long): Category?
    suspend fun insert(category: Category): Long
    suspend fun update(category: Category)
    suspend fun archive(id: Long)
    suspend fun delete(id: Long)
    suspend fun deleteAllUserCreated()
}
