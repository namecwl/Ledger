package com.ledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.app.data.entity.Category
import com.ledger.app.data.entity.Transaction
import com.ledger.app.ui.components.BudgetCard
import com.ledger.app.ui.theme.AmountColors
import com.ledger.app.util.Format
import com.ledger.app.vm.MainViewModel
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BillsScreen(vm: MainViewModel) {

    val categories by vm.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val transactions by vm.monthTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentMonth by vm.selectedMonth.collectAsStateWithLifecycle()
    val budgetState by vm.budgetState.collectAsStateWithLifecycle()

    val catMap = remember(categories) { categories.associateBy { it.id } }

    // 只对当月数据做一次分组
    val grouped = remember(transactions) {
        transactions.groupBy { it.date.take(10) }
            .toList()
            .sortedByDescending { it.first }
    }

    val expense = remember(transactions) {
        transactions.asSequence().filter { it.type == "expense" }.sumOf { it.amount }
    }
    val income = remember(transactions) {
        transactions.asSequence().filter { it.type == "income" }.sumOf { it.amount }
    }
    val balance = income - expense

    var editing by remember { mutableStateOf<Transaction?>(null) }
    var deleting by remember { mutableStateOf<Transaction?>(null) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        // 月份切换
        item(key = "month_header") {
            MonthHeader(
                month = currentMonth,
                onPrev = { vm.selectMonth(currentMonth.minusMonths(1)) },
                onNext = { vm.selectMonth(currentMonth.plusMonths(1)) },
                onToday = { vm.selectMonth(YearMonth.now()) }
            )
        }

        // 汇总
        item(key = "summary") {
            SummaryRow(expense, income, balance)
        }

        // 预算卡
        if (budgetState != null && currentMonth == YearMonth.now()) {
            item(key = "budget") {
                Box(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    BudgetCard(budgetState!!)
                }
            }
        }

        // 账单列表
        if (grouped.isEmpty()) {
            item(key = "empty") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("本月还没有账单", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            grouped.forEach { (day, list) ->
                item(key = "h_$day") {
                    DayHeader(day, list)
                }
                items(list, key = { it.id }) { tx ->
                    BillItem(
                        tx = tx,
                        catMap = catMap,
                        onClick = { editing = tx },
                        onLongClick = { deleting = tx }
                    )
                }
            }
        }
    }

    editing?.let { tx ->
        BillEditDialog(
            tx = tx,
            categories = categories,
            onDismiss = { editing = null },
            onConfirm = { updated ->
                vm.saveTransaction(updated)
                editing = null
            }
        )
    }

    deleting?.let { tx ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除这条账单？") },
            text = { Text("¥${Format.money(tx.amount)}") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteTransaction(tx)
                    deleting = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.ChevronLeft, "上月")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${month.year}年${month.monthValue}月",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (month != YearMonth.now()) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onToday) { Text("回到本月", fontSize = 12.sp) }
            }
        }
        IconButton(
            onClick = onNext,
            enabled = month < YearMonth.now()
        ) {
            Icon(Icons.Default.ChevronRight, "下月")
        }
    }
}

@Composable
private fun SummaryRow(expense: Double, income: Double, balance: Double) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 18.dp),
    ) {
        SummaryItem("支出", expense, AmountColors.Expense, Modifier.weight(1f))
        Box(
            Modifier
                .width(1.dp)
                .height(36.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        SummaryItem("收入", income, AmountColors.Income, Modifier.weight(1f))
        Box(
            Modifier
                .width(1.dp)
                .height(36.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        SummaryItem("结余", balance, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Text(
            Format.money(value),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )
    }
}

@Composable
private fun DayHeader(day: String, list: List<Transaction>) {
    val expense = remember(list) {
        list.asSequence().filter { it.type == "expense" }.sumOf { it.amount }
    }
    val income = remember(list) {
        list.asSequence().filter { it.type == "income" }.sumOf { it.amount }
    }

    val date = remember(day) { LocalDate.parse(day) }
    val weekMap = listOf("一", "二", "三", "四", "五", "六", "日")
    val week = "周" + weekMap[date.dayOfWeek.value - 1]

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Format.shortDay(day),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(Modifier.width(6.dp))
            Text(week, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            buildString {
                if (expense > 0) append("支 ${Format.money(expense)}")
                if (income > 0) {
                    if (isNotEmpty()) append("   ")
                    append("收 ${Format.money(income)}")
                }
            },
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BillItem(
    tx: Transaction,
    catMap: Map<Long, Category>,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val cat = tx.categoryId?.let { catMap[it] }
    val parent = cat?.parentId?.let { catMap[it] }
    val icon = parent?.icon ?: cat?.icon ?: "📝"
    val label = cat?.name ?: "未分类"
    val parentLabel = parent?.name

    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (parentLabel != null) "$parentLabel · $label" else label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = Format.timeOf(tx.date),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!tx.remark.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        tx.remark,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = (if (tx.type == "income") "+" else "-") + Format.money(tx.amount),
                color = if (tx.type == "income") AmountColors.Income
                else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BillEditDialog(
    tx: Transaction,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (Transaction) -> Unit
) {
    var amountText by remember { mutableStateOf(tx.amount.toString()) }
    var remark by remember { mutableStateOf(tx.remark ?: "") }
    var selectedCategory by remember { mutableStateOf(tx.categoryId) }

    val tops = categories.filter { it.parentId == null && it.type == tx.type }
    val subs = categories.filter { it.parentId == selectedCategory }

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
                Text("分类", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tops.forEach { c ->
                        FilterChip(
                            selected = selectedCategory == c.id,
                            onClick = { selectedCategory = c.id },
                            label = { Text(c.name) }
                        )
                    }
                }
                if (subs.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subs.forEach { c ->
                            FilterChip(
                                selected = selectedCategory == c.id,
                                onClick = { selectedCategory = c.id },
                                label = { Text(c.name) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = amountText.toDoubleOrNull() ?: return@TextButton
                onConfirm(
                    tx.copy(
                        amount = amt,
                        remark = remark.ifBlank { null },
                        categoryId = selectedCategory,
                        updatedAt = java.time.LocalDateTime.now().toString()
                    )
                )
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}