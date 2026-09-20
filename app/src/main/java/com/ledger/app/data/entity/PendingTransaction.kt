package com.ledger.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_transactions",
    indices = [Index("status")]
)
data class PendingTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    val rawText: String,
    val parsedAmount: Double? = null,
    val parsedMerchant: String? = null,
    val parsedDate: String? = null,
    val parsedType: String? = null,
    val confidence: Double = 0.0,
    val status: String = "pending",
    val createdAt: String
)