package com.ledger.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ledger.app.data.entity.PendingTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(p: PendingTransaction): Long

    @Update
    suspend fun update(p: PendingTransaction)

    @Delete
    suspend fun delete(p: PendingTransaction)

    @Query("SELECT * FROM pending_transactions WHERE status = 'pending' ORDER BY createdAt DESC")
    fun observePending(): Flow<List<PendingTransaction>>

    @Query("SELECT * FROM pending_transactions WHERE status = 'pending' ORDER BY createdAt DESC")
    suspend fun getPending(): List<PendingTransaction>

    @Query("SELECT COUNT(*) FROM pending_transactions WHERE status = 'pending'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_transactions WHERE status = 'pending'")
    suspend fun countPending(): Int
}