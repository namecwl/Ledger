package com.ledger.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_category_map")
data class MerchantCategoryMap(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantKeyword: String,
    val categoryId: Long,
    val hitCount: Int = 0
)