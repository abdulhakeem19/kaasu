package com.kaasu.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kaasu.app.core.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("UPDATE categories SET isArchived = 1, updatedAt = :now WHERE id = :id")
    suspend fun archive(id: Long, now: Long)

    @Query("DELETE FROM categories WHERE id = :id AND isDefault = 0")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isArchived = 0 ORDER BY isDefault DESC, name ASC")
    fun getAllActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type AND isArchived = 0 ORDER BY isDefault DESC, name ASC")
    fun getByType(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCount(): Int

    // Called by DeleteAllDataUseCase — removes user-created categories; default/seeded ones (isDefault=1) are kept
    @Query("DELETE FROM categories WHERE isDefault = 0")
    suspend fun deleteUserCreated()

    @Query("DELETE FROM categories")
    suspend fun deleteAllIncludingDefaults()

    @Query("SELECT * FROM categories")
    suspend fun getAllForBackup(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReplace(categories: List<CategoryEntity>)
}
