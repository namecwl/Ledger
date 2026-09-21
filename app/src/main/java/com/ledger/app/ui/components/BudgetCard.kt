package com.ledger.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.ui.theme.AmountColors
import com.ledger.app.util.BudgetState
import com.ledger.app.util.Format

@Composable
fun BudgetCard(state: BudgetState, modifier: Modifier = Modifier) {
    val todayColor = when {
        state.todayRemaining < 0 -> AmountColors.Expense
        state.todayRemaining < state.dailyBase * 0.3 -> AmountColors.Warning
        else -> MaterialTheme.colorScheme.primary
    }
    val progressColor = if (state.monthProgress > 0.9f) AmountColors.Expense else MaterialTheme.colorScheme.primary

    LedgerCard(modifier = modifier, contentPadding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("今日可花", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    "¥${Format.money(state.todayAllowance.coerceAtLeast(0.0))}",
                    fontSize = 30.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = todayColor
                )
            }
            Surface(shape = RoundedCornerShape(50), color = progressColor.copy(alpha = 0.11f)) {
                Text(
                    "${Format.percent(state.monthProgress.toDouble())}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = progressColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(state.monthProgress.coerceIn(0f, 1f))
                    .height(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(progressColor)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("本月已花 ¥${Format.money(state.monthSpent)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("预算 ¥${Format.money(state.monthBudget)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MiniStat("今日已花", "¥${Format.money(state.todaySpent)}")
            MiniStat("基础额度", "¥${Format.money(state.dailyBase)}")
            MiniStat("今日剩余", "¥${Format.money(state.todayRemaining)}", if (state.todayRemaining < 0) AmountColors.Expense else null)
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, valueColor: Color? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.labelLarge, color = valueColor ?: MaterialTheme.colorScheme.onSurface)
    }
}