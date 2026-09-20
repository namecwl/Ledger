package com.ledger.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ledger.app.data.entity.MerchantCategoryMap

@Dao
interface MerchantCategoryMapDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(m: MerchantCategoryMap): Long

    @Query("SELECT * FROM merchant_category_map WHERE merchantKeyword = :kw LIMIT 1")
    suspend fun find(kw: String): MerchantCategoryMap?
}