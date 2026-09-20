package com.ledger.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ledger.app.data.entity.AutoRule

@Dao
interface AutoRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(r: AutoRule): Long

    @Query("SELECT * FROM auto_rules WHERE enabled = 1 ORDER BY priority DESC")
    suspend fun getEnabled(): List<AutoRule>

    @Query("SELECT COUNT(*) FROM auto_rules")
    suspend fun count(): Int
}