package com.ledger.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.app.data.entity.Category
import com.ledger.app.data.entity.Transaction
import com.ledger.app.ui.components.EmptyState
import com.ledger.app.ui.components.LedgerCard
import com.ledger.app.ui.components.ScreenHeader
import com.ledger.app.ui.components.SectionTitle
import com.ledger.app.ui.theme.AmountColors
import com.ledger.app.util.Format
import com.ledger.app.vm.MainViewModel
import com.ledger.app.vm.StatsCategoryUi
import com.ledger.app.vm.StatsGranularity

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

private fun paletteColor(index: Int): Color = ChartPalette[index % ChartPalette.size]

private data class CategoryStat(
    val name: String,
    val amount: Double,
    val color: Color,
    val count: Int
)

@Composable
fun StatsScreen(vm: MainViewModel) {
    val stats by vm.statsUi.collectAsStateWithLifecycle()
    val period by vm.statsPeriod.collectAsStateWithLifecycle()
    val granularity by vm.statsGranularity.collectAsStateWithLifecycle()

    var detail by remember { mutableStateOf<StatsCategoryUi?>(null) }

    val selected = detail
    if (selected != null) {
        CategoryDetailScreen(vm = vm, category = selected, periodLabel = period.label, onBack = { detail = null })
        return
    }

    val expenseTotal = stats.expense
    val incomeTotal = stats.income
    val categoryStats = remember(stats.categories) {
        stats.categories.map { category ->
            CategoryStat(
                name = category.name,
                amount = category.amount,
                color = paletteColor(category.colorIndex),
                count = category.count
            )
        }
    }
    val balance = incomeTotal - expenseTotal
    val periodWord = when (granularity) {
        StatsGranularity.DAY -> "当日"
        StatsGranularity.WEEK -> "本周"
        StatsGranularity.MONTH -> "本月"
        StatsGranularity.YEAR -> "本年"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item(key = "header", contentType = "screen_header") {
            ScreenHeader(title = "统计", subtitle = "看清每一笔钱的去向")
        }
        item(key = "granularity", contentType = "granularity") {
            GranularitySelector(
                selected = granularity,
                onSelect = { vm.setStatsGranularity(it) }
            )
        }
        item(key = "period_nav", contentType = "period_nav") {
            PeriodNavigator(
                label = period.label,
                canGoNext = period.canGoNext,
                onPrev = { vm.shiftStatsPeriod(-1) },
                onNext = { vm.shiftStatsPeriod(1) }
            )
        }
        item(key = "summary", contentType = "summary") {
            StatsSummaryCard(periodWord = periodWord, expense = expenseTotal, income = income, balance = balance)
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
                    title = "${periodWord}还没有支出",
                    subtitle = "记录支出后，这里会自动生成分类占比"
                )
            }
        } else {
            item(key = "category_list", contentType = "category_list") {
                LedgerCard(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    stats.categories.forEachIndexed { index, category ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 62.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                            )
                        }
                        val stat = categoryStats[index]
                        CategoryStatRow(
                            rank = index + 1,
                            stat = stat,
                            fraction = if (expenseTotal > 0) stat.amount / expenseTotal else 0.0,
                            onClick = { detail = category }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GranularitySelector(selected: StatsGranularity, onSelect: (StatsGranularity) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatsGranularity.values().forEach { granularity ->
                    val isSelected = granularity == selected
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onSelect(granularity) }
                            .padding(horizontal = 22.dp, vertical = 7.dp)
                    ) {
                        Text(
                            granularity.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodNavigator(label: String, canGoNext: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "上一周期", modifier = Modifier.size(20.dp))
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 6.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = onNext, enabled = canGoNext, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "下一周期", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun StatsSummaryCard(periodWord: String, expense: Double, income: Double, balance: Double) {
    LedgerCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        contentPadding = PaddingValues(20.dp)
    ) {
        Text("${periodWord}支出", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun CategoryStatRow(rank: Int, stat: CategoryStat, fraction: Double, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
        Spacer(Modifier.width(8.dp))
        Text(
            "${Format.percent(fraction)}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "查看${stat.name}明细",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

/** 分类明细下钻页：钱迹风格，展示该分类在当前周期的汇总、子分类占比与逐笔流水 */
@Composable
private fun CategoryDetailScreen(
    vm: MainViewModel,
    category: StatsCategoryUi,
    periodLabel: String,
    onBack: () -> Unit
) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    val stats by vm.statsUi.collectAsStateWithLifecycle()
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val color = paletteColor(category.colorIndex)
    val totalExpense = stats.expense
    val share = if (totalExpense > 0) category.amount / totalExpense else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item(key = "detail_header") {
            ScreenHeader(title = category.name, subtitle = periodLabel, onBack = onBack)
        }
        item(key = "detail_summary") {
            LedgerCard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                contentPadding = PaddingValues(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.13f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${(share * 100).let { kotlin.math.round(it).toInt() }}%", color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("支出合计", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "¥${Format.money(category.amount)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = AmountColors.Expense
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("笔数", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${category.count} 笔", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(color.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(share.toFloat().coerceIn(0f, 1f))
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(color)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "占总支出 ${Format.percent(share)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (category.children.size > 1) {
            item(key = "sub_title") { SectionTitle("子分类") }
            item(key = "sub_list") {
                LedgerCard(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    category.children.forEachIndexed { index, child ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                            )
                        }
                        SubCategoryRow(
                            name = child.name,
                            amount = child.amount,
                            fraction = if (category.amount > 0) child.amount / category.amount else 0.0,
                            color = paletteColor(child.colorIndex + 1)
                        )
                    }
                }
            }
        }

        item(key = "tx_title") { SectionTitle("明细流水（${category.count} 笔）") }
        if (category.transactions.isEmpty()) {
            item(key = "tx_empty") {
                EmptyState(emoji = "🧾", title = "暂无明细", subtitle = "该周期内这个分类还没有账单")
            }
        } else {
            item(key = "tx_list") {
                LedgerCard(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    category.transactions.forEachIndexed { index, tx ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 64.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                            )
                        }
                        DetailTransactionRow(transaction = tx, categoryMap = categoryMap)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubCategoryRow(name: String, amount: Double, fraction: Double, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("¥${Format.money(amount)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(7.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.toFloat().coerceIn(0f, 1f))
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(color)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "${Format.percent(fraction)}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetailTransactionRow(transaction: Transaction, categoryMap: Map<Long, Category>) {
    val category = transaction.categoryId?.let(categoryMap::get)
    val parent = category?.parentId?.let(categoryMap::get)
    val icon = parent?.icon ?: category?.icon ?: "📝"
    val label = when {
        parent != null -> category?.name ?: parent.name
        category != null -> category.name
        else -> "未分类"
    }
    val subtitle = buildString {
        append(Format.timeOf(transaction.date))
        if (!transaction.remark.isNullOrBlank()) append(" · ${transaction.remark}")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 17.sp)
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "-¥${Format.money(transaction.amount)}",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
    }
}
