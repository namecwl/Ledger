package com.ledger.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("date"),
        Index("categoryId"),
        Index("accountId"),
        Index("type"),
        Index(value = ["sourceKey"], unique = true)
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceKey: String? = null,
    val type: String,
    val amount: Double,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val date: String,
    val remark: String? = null,
    val relatedKey: String? = null,
    val rawCategory: String? = null,
    val rawType: String? = null,
    val createdAt: String,
    val updatedAt: String
)