package com.ledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.ledger.app.vm.MainViewModel
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun BillsScreen(vm: MainViewModel) {

    val transactions by vm.transactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by vm.categories.collectAsStateWithLifecycle(initialValue = emptyList())

    val catMap = remember(categories) { categories.associateBy { it.id } }

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val monthStr = currentMonth.toString()

    val monthTx = remember(transactions, monthStr) {
        transactions.filter { it.date.startsWith(monthStr) }
    }

    val expense = monthTx.filter { it.type == "expense" }.sumOf { it.amount }
    val income = monthTx.filter { it.type == "income" }.sumOf { it.amount }
    val balance = income - expense

    val grouped = remember(monthTx) {
        monthTx.groupBy { it.date.substring(0, 10) }
            .toList()
            .sortedByDescending { it.first }
    }

    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // === 顶部汇总卡片 ===
        Card(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Icon(Icons.Default.ChevronLeft, "上月")
                    }
                    Text(
                        "${currentMonth.year}年${currentMonth.monthValue}月",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = { currentMonth = currentMonth.plusMonths(1) },
                        enabled = currentMonth < YearMonth.now()
                    ) {
                        Icon(Icons.Default.ChevronRight, "下月")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    SummaryItem(
                        "支出",
                        "¥%.2f".format(expense),
                        Color(0xFFE53935),
                        Modifier.weight(1f)
                    )
                    SummaryItem(
                        "收入",
                        "¥%.2f".format(income),
                        Color(0xFF43A047),
                        Modifier.weight(1f)
                    )
                    SummaryItem(
                        "结余",
                        "¥%.2f".format(balance),
                        MaterialTheme.colorScheme.onPrimaryContainer,
                        Modifier.weight(1f)
                    )
                }
            }
        }

        // === 列表 ===
        LazyColumn(Modifier.fillMaxSize()) {
            if (grouped.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "本月还没有账单",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                grouped.forEach { (day, list) ->
                    item(key = "header_$day") {
                        DayHeader(day, list)
                    }
                    items(list, key = { it.id }) { tx ->
                        BillItem(
                            tx = tx,
                            catMap = catMap,
                            onClick = { pendingDelete = tx }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这条账单？") },
            text = { Text("¥%.2f".format(tx.amount)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteTransaction(tx)
                    pendingDelete = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(value, color = color, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
private fun DayHeader(day: String, list: List<Transaction>) {
    val expense = list.filter { it.type == "expense" }.sumOf { it.amount }
    val income = list.filter { it.type == "income" }.sumOf { it.amount }
    val date = LocalDate.parse(day)
    val weekMap = listOf("一", "二", "三", "四", "五", "六", "日")
    val week = "周" + weekMap[date.dayOfWeek.value - 1]

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                day.substring(5),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(Modifier.width(6.dp))
            Text(week, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val parts = buildList {
            if (expense > 0) add("支 ¥%.2f".format(expense))
            if (income > 0) add("收 ¥%.2f".format(income))
        }
        Text(
            parts.joinToString("   "),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BillItem(
    tx: Transaction,
    catMap: Map<Long, Category>,
    onClick: () -> Unit
) {
    val cat = tx.categoryId?.let { catMap[it] }
    val parent = cat?.parentId?.let { catMap[it] }
    val icon = parent?.icon ?: cat?.icon ?: "📝"
    val label = cat?.name ?: "未分类"
    val parentLabel = parent?.name

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 20.sp)
        }
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = if (parentLabel != null) "$parentLabel · $label" else label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
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
            text = (if (tx.type == "income") "+" else "-") + "%.2f".format(tx.amount),
            color = if (tx.type == "income") Color(0xFF43A047)
            else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
    }
}