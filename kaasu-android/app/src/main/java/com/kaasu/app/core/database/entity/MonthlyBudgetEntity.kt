package com.kaasu.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "monthly_budgets",
    indices = [Index(value = ["month", "year"], unique = true)]
)
data class MonthlyBudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val month: Int,
    val year: Int,
    val amountInPaise: Long,
    val createdAt: Long,
    val updatedAt: Long
)
