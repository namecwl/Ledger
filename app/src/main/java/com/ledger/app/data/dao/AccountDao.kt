package com.ledger.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ledger.app.data.entity.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(a: Account): Long

    @Update
    suspend fun update(a: Account)

    @Delete
    suspend fun delete(a: Account)

    @Query("SELECT * FROM accounts ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<Account>>

    @Query("SELECT * FROM accounts ORDER BY sortOrder, id")
    suspend fun observeAllOnce(): List<Account>

    @Query("SELECT * FROM accounts WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Account?

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int
}