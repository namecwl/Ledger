package com.ledger.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.app.data.entity.Transaction
import com.ledger.app.ui.components.EmptyState
import com.ledger.app.ui.components.LedgerCard
import com.ledger.app.ui.components.ScreenHeader
import com.ledger.app.ui.components.SectionTitle
import com.ledger.app.ui.theme.AmountColors
import com.ledger.app.util.Format
import com.ledger.app.vm.MainViewModel
import java.time.YearMonth

private val ChartPalette = listOf(
    Color(0xFF16A36A),
    Color(0xFF4E7DF2),
    Color(0xFFF09A3E),
    Color(0xFFE85D55),
    Color(0xFF8B6FE8),
    Color(0xFF2AA7A0),
    Color(0xFFE77AA5),
    Color(0xFF8B949E)
)

private data class CategoryStat(
    val name: String,
    val amount: Double,
    val color: Color
)

@Composable
fun StatsScreen(vm: MainViewModel) {
    val transactions by vm.monthTransactions.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val selectedMonth by vm.selectedMonth.collectAsStateWithLifecycle()

    val statsData = remember(transactions, categories) {
        val categoryMap = categories.associateBy { it.id }
        var expenseTotal = 0.0
        var incomeTotal = 0.0
        val grouped = HashMap<String, Double>()

        for (transaction in transactions) {
            when (transaction.type) {
                "expense" -> {
                    expenseTotal += transaction.amount
                    val category = transaction.categoryId?.let(categoryMap::get)
                    val parent = category?.parentId?.let(categoryMap::get)
                    val name = parent?.name ?: category?.name ?: "未分类"
                    grouped[name] = (grouped[name] ?: 0.0) + transaction.amount
                }
                "income", "refund", "reimbursement" -> incomeTotal += transaction.amount
            }
        }

        val sorted = grouped.entries
            .sortedByDescending { it.value }
            .mapIndexed { index, entry ->
                CategoryStat(entry.key, entry.value, ChartPalette[index % ChartPalette.size])
            }
        Triple(expenseTotal, incomeTotal, sorted)
    }
    val expenseTotal = statsData.first
    val incomeTotal = statsData.second
    val categoryStats = statsData.third
    val balance = incomeTotal - expenseTotal

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item(key = "header", contentType = "screen_header") {
            ScreenHeader(
                title = "统计",
                subtitle = "看清每一笔钱的去向",
                trailing = { MonthControl(month = selectedMonth, onPrev = { vm.selectMonth(selectedMonth.minusMonths(1)) }, onNext = { vm.selectMonth(selectedMonth.plusMonths(1)) }, canGoNext = selectedMonth < YearMonth.now()) }
            )
        }
        item(key = "summary", contentType = "summary") {
            StatsSummaryCard(expense = expenseTotal, income = incomeTotal, balance = balance)
        }
        item(key = "chart", contentType = "chart") {
            BreakdownChartCard(stats = categoryStats, expenseTotal = expenseTotal)
        }
        item(key = "list_title", contentType = "section_title") {
            SectionTitle("支出分类")
        }

        if (categoryStats.isEmpty()) {
            item(key = "empty", contentType = "empty") {
                EmptyState(
                    emoji = "📊",
                    title = "本月还没有支出",
                    subtitle = "记录支出后，这里会自动生成分类占比"
                )
            }
        } else {
            itemsIndexed(categoryStats, key = { _, stat -> stat.name }, contentType = { _, _ -> "category_stat" }) { index, stat ->
                CategoryStatCard(
                    rank = index + 1,
                    stat = stat,
                    fraction = if (expenseTotal > 0) stat.amount / expenseTotal else 0.0
                )
            }
        }
    }
}

@Composable
private fun MonthControl(
    month: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    canGoNext: Boolean
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "上月", modifier = Modifier.size(19.dp))
            }
            Text(
                "${month.monthValue}月",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            IconButton(onClick = onNext, enabled = canGoNext, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronRight, contentDescription = "下月", modifier = Modifier.size(19.dp))
            }
        }
    }
}

@Composable
private fun StatsSummaryCard(expense: Double, income: Double, balance: Double) {
    LedgerCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        contentPadding = PaddingValues(20.dp)
    ) {
        Text("本月支出", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(
            "¥${Format.money(expense)}",
            style = MaterialTheme.typography.displaySmall,
            color = AmountColors.Expense,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(18.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryValue("收入", income, AmountColors.Income, Modifier.weight(1f))
            SummaryValue("结余", balance, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryValue(label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(3.dp))
        Text("¥${Format.money(amount)}", style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun BreakdownChartCard(stats: List<CategoryStat>, expenseTotal: Double) {
    LedgerCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        Text("分类占比", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        if (stats.isEmpty() || expenseTotal <= 0.0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DonutChart(stats = stats, total = expenseTotal)
                Spacer(Modifier.width(18.dp))
                Column(modifier = Modifier.weight(1f)) {
                    stats.take(4).forEach { stat ->
                        LegendRow(stat = stat, total = expenseTotal)
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DonutChart(stats: List<CategoryStat>, total: Double) {
    Box(modifier = Modifier.size(142.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 18.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = Color(0xFFE7EEE9),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            var startAngle = -90f
            stats.forEach { stat ->
                val sweep = (stat.amount / total * 360.0).toFloat()
                drawArc(
                    color = stat.color,
                    startAngle = startAngle + 1.2f,
                    sweepAngle = (sweep - 2.4f).coerceAtLeast(0f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("总支出", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("¥${Format.money(total)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun LegendRow(stat: CategoryStat, total: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(stat.color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            stat.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "${Format.percent(stat.amount / total)}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoryStatCard(rank: Int, stat: CategoryStat, fraction: Double) {
    LedgerCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(stat.color.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Text("$rank", color = stat.color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stat.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "¥${Format.money(stat.amount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(stat.color.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.toFloat().coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(stat.color)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "${Format.percent(fraction)}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}