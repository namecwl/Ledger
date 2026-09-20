package com.ledger.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.app.data.entity.Transaction
import com.ledger.app.vm.MainViewModel

@Composable
fun BillsScreen(vm: MainViewModel) {

    val transactions by vm.transactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by vm.categories.collectAsStateWithLifecycle(initialValue = emptyList())

    val catMap = remember(categories) { categories.associateBy { it.id } }
    val grouped = remember(transactions) {
        transactions.groupBy { it.date.substring(0, 10) }
    }

    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }

    LazyColumn(Modifier.fillMaxSize()) {
        grouped.forEach { (day, list) ->
            item(key = "header_$day") {
                val expense = list.filter { it.type == "expense" }.sumOf { it.amount }
                val income = list.filter { it.type == "income" }.sumOf { it.amount }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(day, fontWeight = FontWeight.Bold)
                    Text(
                        "支 ¥%.2f   收 ¥%.2f".format(expense, income),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            items(list, key = { it.id }) { tx ->
                val cat = tx.categoryId?.let { catMap[it] }
                val parent = cat?.parentId?.let { catMap[it] }
                val label = when {
                    parent != null && cat != null -> "${parent.name} / ${cat.name}"
                    cat != null -> cat.name
                    else -> "未分类"
                }
                ListItem(
                    headlineContent = { Text(label) },
                    supportingContent = {
                        tx.remark?.takeIf { it.isNotBlank() }?.let { Text(it) }
                    },
                    trailingContent = {
                        Text(
                            text = (if (tx.type == "income") "+" else "-") + "¥%.2f".format(tx.amount),
                            color = if (tx.type == "income") Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.clickable { pendingDelete = tx }
                )
                HorizontalDivider()
            }
        }

        if (transactions.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("还没有记账，去「记一笔」添加")
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