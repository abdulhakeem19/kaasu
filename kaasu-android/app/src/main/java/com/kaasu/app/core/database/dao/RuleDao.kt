package com.kaasu.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kaasu.app.core.database.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RuleEntity): Long

    @Update
    suspend fun update(rule: RuleEntity)

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM rules ORDER BY priority DESC, id ASC")
    fun getAll(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE isActive = 1 ORDER BY priority DESC, id ASC")
    fun getActiveByPriority(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getById(id: Long): RuleEntity?

    @Query("SELECT * FROM rules WHERE isActive = 1 ORDER BY priority DESC, id ASC")
    suspend fun getActiveRulesList(): List<RuleEntity>

    // Called by DeleteAllDataUseCase — keeps system rules (priority=10) and removes only user rules (priority>=100)
    @Query("DELETE FROM rules WHERE priority >= 100")
    suspend fun deleteUserRules()

    @Query("DELETE FROM rules")
    suspend fun deleteAllRules()

    @Query("SELECT * FROM rules")
    suspend fun getAllForBackup(): List<RuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<RuleEntity>)
}
