package com.kaasu.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String?,
    val matchText: String,
    val matchType: String,
    val categoryId: Long?,
    val transactionType: String?,
    val sourceAppPackage: String?,
    val priority: Int = 0,
    val isSystem: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long
)
