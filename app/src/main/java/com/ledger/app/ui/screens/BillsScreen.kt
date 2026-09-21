package com.ledger.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.app.data.entity.Category
import com.ledger.app.data.entity.Transaction
import com.ledger.app.ui.components.BudgetCard
import com.ledger.app.ui.components.EmptyState
import com.ledger.app.ui.components.LedgerCard
import com.ledger.app.ui.components.ScreenHeader
import com.ledger.app.ui.theme.AmountColors
import com.ledger.app.util.Format
import com.ledger.app.vm.BillDayUi
import com.ledger.app.vm.MainViewModel
import java.time.YearMonth

private val InflowTypes = setOf("income", "refund", "reimbursement")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BillsScreen(vm: MainViewModel) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    val bills by vm.billsUi.collectAsStateWithLifecycle()
    val currentMonth by vm.selectedMonth.collectAsStateWithLifecycle()
    val budgetState by vm.budgetState.collectAsStateWithLifecycle()

    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val days = bills.days
    val totals = bills.expense to bills.income

    var editing by remember { mutableStateOf<Transaction?>(null) }
    var deleting by remember { mutableStateOf<Transaction?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item(key = "header", contentType = "header") {
            ScreenHeader(
                title = "账单",
                subtitle = "每一笔收支，都清晰可见",
                trailing = {
                    if (currentMonth != YearMonth.now()) {
                        IconButton(onClick = { vm.selectMonth(YearMonth.now()) }) {
                            Icon(Icons.Default.Today, contentDescription = "回到本月", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
        item(key = "month_nav", contentType = "month_nav") {
            MonthNavigator(
                month = currentMonth,
                onPrev = { vm.selectMonth(currentMonth.minusMonths(1)) },
                onNext = { vm.selectMonth(currentMonth.plusMonths(1)) },
                canGoNext = currentMonth < YearMonth.now()
            )
        }
        item(key = "summary", contentType = "summary") {
            SummaryHero(
                month = currentMonth,
                expense = totals.first,
                income = totals.second
            )
        }
        if (budgetState != null && currentMonth == YearMonth.now()) {
            item(key = "budget", contentType = "budget") {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    BudgetCard(budgetState!!)
                }
            }
        }
        if (days.isEmpty()) {
            item(key = "empty", contentType = "empty") {
                EmptyState(
                    emoji = "🧾",
                    title = "本月还没有账单",
                    subtitle = "记下第一笔，月度概览就会在这里出现"
                )
            }
        } else {
            days.forEach { day ->
                item(key = "day_${day.date}", contentType = "day_header") {
                    DayHeader(day)
                }
                items(day.transactions, key = { it.id }, contentType = { "bill" }) { transaction ->
                    BillRow(
                        transaction = transaction,
                        categoryMap = categoryMap,
                        onClick = { editing = transaction },
                        onLongClick = { deleting = transaction }
                    )
                }
            }
        }
    }

    editing?.let { transaction ->
        BillEditDialog(
            transaction = transaction,
            categories = categories,
            onDismiss = { editing = null },
            onConfirm = { updated ->
                vm.saveTransaction(updated)
                editing = null
            }
        )
    }

    deleting?.let { transaction ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除这条账单？") },
            text = { Text("${transaction.remark ?: "无备注"} · ¥${Format.money(transaction.amount)}") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteTransaction(transaction)
                    deleting = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun MonthNavigator(
    month: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    canGoNext: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "上月")
                }
                Text(
                    "${month.year}年${month.monthValue}月",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = onNext, enabled = canGoNext) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "下月")
                }
            }
        }
    }
}

@Composable
private fun SummaryHero(month: YearMonth, expense: Double, income: Double) {
    val balance = income - expense
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, Color(0xFF0C7D51))
                )
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column {
            Text(
                "${month.monthValue}月结余",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.76f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "¥${Format.money(balance)}",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryMetric("支出", expense, Modifier.weight(1f))
                SummaryMetric("收入", income, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, amount: Double, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f))
        Spacer(Modifier.height(3.dp))
        Text("¥${Format.money(amount)}", style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

@Composable
private fun DayHeader(day: BillDayUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Format.shortDay(day.date), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.width(6.dp))
            Text(day.week, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val summary = buildString {
            if (day.expense > 0) append("支 ¥${Format.money(day.expense)}")
            if (day.income > 0) {
                if (isNotEmpty()) append("  ")
                append("收 ¥${Format.money(day.income)}")
            }
        }
        Text(summary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BillRow(
    transaction: Transaction,
    categoryMap: Map<Long, Category>,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val category = transaction.categoryId?.let(categoryMap::get)
    val parent = category?.parentId?.let(categoryMap::get)
    val icon = parent?.icon ?: category?.icon ?: "📝"
    val label = category?.name ?: "未分类"
    val parentLabel = parent?.name
    val inflow = transaction.type in InflowTypes
    val amountColor = when (transaction.type) {
        "refund", "reimbursement" -> AmountColors.Refund
        "income" -> AmountColors.Income
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (inflow) AmountColors.Income.copy(alpha = 0.11f)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (parentLabel != null) "$parentLabel · $label" else label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1
            )
            Spacer(Modifier.height(3.dp))
            Text(
                buildString {
                    append(Format.timeOf(transaction.date))
                    if (!transaction.remark.isNullOrBlank()) append(" · ${transaction.remark}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            (if (inflow) "+" else "-") + "¥" + Format.money(transaction.amount),
            color = amountColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 70.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BillEditDialog(
    transaction: Transaction,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (Transaction) -> Unit
) {
    var amountText by remember { mutableStateOf(transaction.amount.toString()) }
    var remark by remember { mutableStateOf(transaction.remark ?: "") }
    var selectedCategory by remember { mutableStateOf(transaction.categoryId) }
    val tops = remember(categories, transaction.type) {
        categories.filter { it.parentId == null && it.type == transaction.type }
    }
    val subs = remember(categories, selectedCategory) {
        categories.filter { it.parentId == selectedCategory }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑账单") },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("金额") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("备注") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("分类", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tops.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category.id,
                            onClick = { selectedCategory = category.id },
                            label = { Text(category.name) }
                        )
                    }
                }
                if (subs.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subs.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category.id,
                                onClick = { selectedCategory = category.id },
                                label = { Text(category.name) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountText.toDoubleOrNull() ?: return@TextButton
                onConfirm(
                    transaction.copy(
                        amount = amount,
                        remark = remark.ifBlank { null },
                        categoryId = selectedCategory,
                        updatedAt = Format.nowIso()
                    )
                )
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}