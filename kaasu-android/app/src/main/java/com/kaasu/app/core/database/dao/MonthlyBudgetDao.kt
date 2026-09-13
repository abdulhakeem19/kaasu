package com.kaasu.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kaasu.app.core.database.entity.MonthlyBudgetEntity

@Dao
interface MonthlyBudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: MonthlyBudgetEntity): Long

    @Update
    suspend fun update(budget: MonthlyBudgetEntity)

    @Query("SELECT * FROM monthly_budgets WHERE month = :month AND year = :year LIMIT 1")
    suspend fun getByMonthYear(month: Int, year: Int): MonthlyBudgetEntity?
}
