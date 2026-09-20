package com.ledger.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val month: String,       // "2026-09"
    val amount: Double,      // 月预算
    val createdAt: String,
    val updatedAt: String
)