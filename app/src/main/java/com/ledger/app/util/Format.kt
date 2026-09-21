package com.ledger.app.util

import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object Format {
    private val isoSeconds = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    private val monthDayTime = DateTimeFormatter.ofPattern("MM-dd HH:mm")
    private val shortDayFormatter = DateTimeFormatter.ofPattern("MM-dd")
    private val dateOnly = DateTimeFormatter.ISO_LOCAL_DATE
    private val moneyFormat = ThreadLocal.withInitial {
        DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.getDefault())).apply {
            roundingMode = RoundingMode.HALF_UP
        }
    }
    private val percentFormat = ThreadLocal.withInitial {
        DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.getDefault())).apply {
            roundingMode = RoundingMode.HALF_UP
        }
    }

    fun money(amount: Double?): String {
        val value = amount ?: 0.0
        return moneyFormat.get().format(value)
    }

    fun percent(value: Double): String = percentFormat.get().format(value * 100.0)

    fun nowIso(): String = LocalDateTime.now().format(isoSeconds)

    fun timeOf(dateStr: String): String {
        val parsed = parseDateTime(dateStr) ?: return dateStr
        return parsed.format(monthDayTime)
    }

    fun monthDayTime(value: LocalDateTime): String = value.format(monthDayTime)

    fun iso(value: LocalDateTime): String = value.format(isoSeconds)

    fun date(dateStr: String): String = dateStr.take(10).ifBlank { dateStr }

    fun shortDay(dateStr: String): String {
        return runCatching { LocalDate.parse(dateStr.take(10), dateOnly).format(shortDayFormatter) }
            .getOrDefault(dateStr)
    }

    private fun parseDateTime(dateStr: String): LocalDateTime? {
        return runCatching { LocalDateTime.parse(dateStr) }
            .recoverCatching { LocalDateTime.parse(dateStr.take(19), isoSeconds) }
            .getOrNull()
    }
}