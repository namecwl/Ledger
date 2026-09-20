package com.ledger.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String = "other",
    val balance: Double = 0.0,
    val icon: String? = null,
    val sortOrder: Int = 0,
    val isBuiltin: Boolean = false
)