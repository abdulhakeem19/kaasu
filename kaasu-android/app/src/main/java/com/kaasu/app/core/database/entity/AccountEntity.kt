package com.kaasu.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [Index("lastFourDigits")]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val lastFourDigits: String?,
    val accountType: String,
    val colorArgb: Int?,
    val isActive: Boolean = true,
    val createdAt: Long
)
