package com.ledger.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auto_rules")
data class AutoRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourcePackage: String,
    val pattern: String,
    val amountGroup: Int = 1,
    val merchantGroup: Int? = null,
    val typeGroup: Int? = null,
    val categoryId: Long? = null,
    val priority: Int = 0,
    val enabled: Boolean = true,
    val updatedAt: String
)