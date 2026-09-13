package com.kaasu.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kaasu.app.core.database.entity.IgnoredPatternEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IgnoredPatternDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(pattern: IgnoredPatternEntity): Long

    @Query("DELETE FROM ignored_patterns WHERE id = :id AND isSystem = 0")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM ignored_patterns ORDER BY isSystem DESC, pattern ASC")
    fun getAll(): Flow<List<IgnoredPatternEntity>>

    @Query("SELECT pattern FROM ignored_patterns")
    suspend fun getAllPatterns(): List<String>
}
