package com.ledger.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.app.vm.MainViewModel
import java.time.LocalDate

@Composable
fun StatsScreen(vm: MainViewModel) {

    val transactions by vm.transactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by vm.categories.collectAsStateWithLifecycle(initialValue = emptyList())

    val catMap = remember(categories) { categories.associateBy { it.id } }
    val month = remember { LocalDate.now().toString().substring(0, 7) }

    val monthTx = transactions.filter { it.date.startsWith(month) }
    val expense = monthTx.filter { it.type == "expense" }.sumOf { it.amount }
    val income = monthTx.filter { it.type == "income" }.sumOf { it.amount }
    val balance = income - expense

    val byParent = monthTx
        .filter { it.type == "expense" }
        .groupBy { tx ->
            val cat = tx.categoryId?.let { catMap[it] }
            val parent = cat?.parentId?.let { catMap[it] }
            parent?.name ?: cat?.name ?: "未分类"
        }
        .mapValues { (_, list) -> list.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(month, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("支出", style = MaterialTheme.typography.labelMedium)
                    Text("¥%.2f".format(expense), style = MaterialTheme.typography.titleMedium)
                }
                Column {
                    Text("收入", style = MaterialTheme.typography.labelMedium)
                    Text("¥%.2f".format(income), style = MaterialTheme.typography.titleMedium)
                }
                Column {
                    Text("结余", style = MaterialTheme.typography.labelMedium)
                    Text("¥%.2f".format(balance), style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("分类占比", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
        }

        items(byParent) { (name, amt) ->
            val pct = if (expense > 0) amt / expense else 0.0
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name)
                    Text("¥%.2f  (%.1f%%)".format(amt, pct * 100))
                }
                LinearProgressIndicator(
                    progress = { pct.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }
    }
}