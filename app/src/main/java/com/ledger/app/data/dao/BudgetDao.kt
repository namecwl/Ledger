package com.ledger.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ledger.app.data.entity.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget): Long

    @Query("SELECT * FROM budgets WHERE month = :month LIMIT 1")
    suspend fun getByMonth(month: String): Budget?

    @Query("SELECT * FROM budgets WHERE month = :month LIMIT 1")
    fun observeByMonth(month: String): Flow<Budget?>
}