package com.kaasu.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kaasu.app.core.database.entity.SmsSenderEntity
import kotlinx.coroutines.flow.Flow

// Mirrors AppSourceDao's query shapes, keyed on senderId instead of packageName.
@Dao
interface SmsSenderDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(sender: SmsSenderEntity): Long

    @Update
    suspend fun update(sender: SmsSenderEntity)

    @Query("SELECT * FROM sms_senders ORDER BY senderId ASC")
    fun getAll(): Flow<List<SmsSenderEntity>>

    @Query("SELECT * FROM sms_senders WHERE senderId = :senderId LIMIT 1")
    suspend fun getBySenderId(senderId: String): SmsSenderEntity?

    @Query("UPDATE sms_senders SET lastSeenAt = :time, updatedAt = :time WHERE senderId = :senderId")
    suspend fun updateLastSeen(senderId: String, time: Long)
}
