package com.kaasu.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kaasu.app.core.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Query("SELECT * FROM accounts ORDER BY displayName ASC")
    fun getAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE lastFourDigits = :lastFour AND isActive = 1")
    suspend fun getByLastFour(lastFour: String): List<AccountEntity>

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Retires an account without destroying the money on it.
     *
     * A hard delete leaves `transactions.accountId` pointing at an id that no longer exists, so
     * those rows silently drop out of every per-account figure with no error anywhere — the money
     * appears to have never happened. Deactivating keeps the history intact and readable.
     */
    @Query("UPDATE accounts SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)

    /**
     * Records what the bank itself last said this balance was.
     *
     * Advisory: shown next to the derived figure so a disagreement is visible. Never used as the
     * balance — that would quietly paper over every message Kaasu failed to capture.
     */
    @Query("UPDATE accounts SET lastStatedBalanceInPaise = :balanceInPaise, lastStatedBalanceAt = :at WHERE id = :id")
    suspend fun setLastStatedBalance(id: Long, balanceInPaise: Long, at: Long)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()

    @Query("SELECT * FROM accounts")
    suspend fun getAllForBackup(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)
}
