package com.kaasu.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ignored_patterns")
data class IgnoredPatternEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pattern: String,
    val reason: String?,
    val sourceAppPackage: String?,
    val isSystem: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
