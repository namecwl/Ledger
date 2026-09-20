package com.ledger.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Format {
    fun money(amount: Double?): String {
        if (amount == null) return "0.00"
        return String.format(Locale.getDefault(), "%.2f", amount)
    }

    fun timeOf(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateStr) ?: Date()
            outputFormat.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    fun date(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(dateStr) ?: Date()
            outputFormat.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }
}
