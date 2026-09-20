package com.ledger.app.util

import com.ledger.app.data.entity.Transaction
import java.time.LocalDate

data class BudgetState(
    val monthBudget: Double,
    val monthSpent: Double,
    val monthRemaining: Double,
    val daysInMonth: Int,
    val dayOfMonth: Int,
    val dailyBase: Double,
    val todayAllowance: Double,
    val todaySpent: Double,
    val todayRemaining: Double,
    val monthProgress: Float
)

object BudgetCalculator {

    /**
     * 逐日累计模型：
     *   dailyBase = monthBudget / daysInMonth
     *   available(d) = dailyBase + carryOver(d-1)
     *   carryOver(d) = available(d) - spent(d)
     * 今日可花 = dailyBase + 昨日结余
     */
    fun compute(
        monthBudget: Double,
        monthTransactions: List<Transaction>,
        today: LocalDate = LocalDate.now()
    ): BudgetState {
        val daysInMonth = today.lengthOfMonth()
        val dayOfMonth = today.dayOfMonth
        val dailyBase = if (monthBudget > 0) monthBudget / daysInMonth else 0.0

        val expenseByDay: Map<String, Double> = monthTransactions
            .asSequence()
            .filter { it.type == "expense" }
            .groupBy { it.date.take(10) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        var carryOver = 0.0
        for (d in 1 until dayOfMonth) {
            val dayStr = today.withDayOfMonth(d).toString()
            val spent = expenseByDay[dayStr] ?: 0.0
            val available = dailyBase + carryOver
            carryOver = available - spent
        }

        val todayAllowance = dailyBase + carryOver
        val todaySpent = expenseByDay[today.toString()] ?: 0.0
        val todayRemaining = todayAllowance - todaySpent

        val monthSpent = expenseByDay.values.sum()
        val monthRemaining = monthBudget - monthSpent
        val progress = if (monthBudget > 0) {
            (monthSpent / monthBudget).toFloat().coerceIn(0f, 1f)
        } else 0f

        return BudgetState(
            monthBudget = monthBudget,
            monthSpent = monthSpent,
            monthRemaining = monthRemaining,
            daysInMonth = daysInMonth,
            dayOfMonth = dayOfMonth,
            dailyBase = dailyBase,
            todayAllowance = todayAllowance,
            todaySpent = todaySpent,
            todayRemaining = todayRemaining,
            monthProgress = progress
        )
    }
}