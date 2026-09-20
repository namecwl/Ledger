package com.ledger.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ledger.app.data.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(t: Transaction): Long

    @Update
    suspend fun update(t: Transaction)

    @Delete
    suspend fun delete(t: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<Transaction>>

    /** 按月查询：start = "2026-09-01"，end = "2026-10-01" */
    @Query("SELECT * FROM transactions WHERE date >= :start AND date < :end ORDER BY date DESC, id DESC")
    fun observeByRange(start: String, end: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date >= :start AND date < :end ORDER BY date DESC, id DESC")
    suspend fun getByRangeOnce(start: String, end: String): List<Transaction>

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    suspend fun observeAllOnce(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE sourceKey = :key LIMIT 1")
    suspend fun findBySourceKey(key: String): Transaction?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int
}