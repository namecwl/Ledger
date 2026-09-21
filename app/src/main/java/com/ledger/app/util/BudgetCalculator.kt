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
        val expenseByDay = HashMap<String, Double>(daysInMonth)

        for (transaction in monthTransactions) {
            if (transaction.type != "expense") continue
            val day = transaction.date.take(10)
            expenseByDay[day] = (expenseByDay[day] ?: 0.0) + transaction.amount
        }

        var carryOver = 0.0
        for (day in 1 until dayOfMonth) {
            val dayStr = today.withDayOfMonth(day).toString()
            val spent = expenseByDay[dayStr] ?: 0.0
            carryOver = dailyBase + carryOver - spent
        }

        val todayAllowance = dailyBase + carryOver
        val todaySpent = expenseByDay[today.toString()] ?: 0.0
        val monthSpent = expenseByDay.values.sum()
        val monthRemaining = monthBudget - monthSpent
        val progress = if (monthBudget > 0) {
            (monthSpent / monthBudget).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }

        return BudgetState(
            monthBudget = monthBudget,
            monthSpent = monthSpent,
            monthRemaining = monthRemaining,
            daysInMonth = daysInMonth,
            dayOfMonth = dayOfMonth,
            dailyBase = dailyBase,
            todayAllowance = todayAllowance,
            todaySpent = todaySpent,
            todayRemaining = todayAllowance - todaySpent,
            monthProgress = progress
        )
    }
}