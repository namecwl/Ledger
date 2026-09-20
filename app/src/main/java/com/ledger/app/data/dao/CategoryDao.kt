package com.ledger.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ledger.app.data.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(c: Category): Long

    @Update
    suspend fun update(c: Category)

    @Delete
    suspend fun delete(c: Category)

    @Query("SELECT * FROM categories ORDER BY type, sortOrder, id")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY type, sortOrder, id")
    suspend fun observeAllOnce(): List<Category>

    @Query("SELECT * FROM categories WHERE parentId IS NULL AND type = :type ORDER BY sortOrder, id")
    suspend fun getTops(type: String): List<Category>

    @Query("SELECT * FROM categories WHERE parentId = :pid ORDER BY sortOrder, id")
    suspend fun getChildren(pid: Long): List<Category>

    @Query("SELECT * FROM categories WHERE name = :name AND parentId IS :pid AND type = :type LIMIT 1")
    suspend fun find(name: String, pid: Long?, type: String): Category?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}