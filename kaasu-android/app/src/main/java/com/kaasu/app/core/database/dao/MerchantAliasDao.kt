package com.kaasu.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kaasu.app.core.database.entity.MerchantAliasEntity

@Dao
interface MerchantAliasDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alias: MerchantAliasEntity)

    @Query("SELECT displayName FROM merchant_aliases WHERE originalLower = :originalLower LIMIT 1")
    suspend fun getDisplayName(originalLower: String): String?

    @Query("SELECT * FROM merchant_aliases")
    suspend fun getAll(): List<MerchantAliasEntity>

    @Query("DELETE FROM merchant_aliases")
    suspend fun deleteAll()
}
