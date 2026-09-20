package com.ledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import com.ledger.app.data.entity.PendingTransaction
import com.ledger.app.util.Format
import com.ledger.app.vm.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PendingScreen(vm: MainViewModel) {

    val list by vm.pendingList.collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by vm.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val accounts by vm.accounts.collectAsStateWithLifecycle(initialValue = emptyList())

    var confirming by remember { mutableStateOf<PendingTransaction?>(null) }

    if (list.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("暂无待确认账单", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text(
                    "开启通知监听和无障碍服务后\n支付时会自动抓取",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        items(list, key = { it.id }) { p ->
            PendingItem(
                p = p,
                onConfirm = { confirming = p },
                onReject = { vm.rejectPending(p) },
                onDelete = { vm.deletePending(p) }
            )
        }
    }

    confirming?.let { p ->
        ConfirmDialog(
            p = p,
            categories = categories,
            accounts = accounts,
            onDismiss = { confirming = null },
            onConfirm = { cat, acc ->
                vm.confirmPending(p, cat, acc)
                confirming = null
            }
        )
    }
}

@Composable
private fun PendingItem(
    p: PendingTransaction,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    onDelete: () -> Unit
) {
    val confColor = when {
        p.confidence >= 0.8 -> Color(0xFF43A047)
        p.confidence >= 0.5 -> Color(0xFFFB8C00)
        else -> Color(0xFFE53935)
    }

    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(confColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                when (p.source) {
                    "notification" -> "通"
                    "accessibility" -> "障"
                    else -> "?"
                },
                color = confColor,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                p.parsedMerchant ?: "未知商户",
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${Format.timeOf(p.parsedDate ?: "")} · 置信度 ${(p.confidence * 100).toInt()}%",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            "¥${Format.money(p.parsedAmount ?: 0.0)}",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Color(0xFFE53935)
        )
        Spacer(Modifier.width(8.dp))

        IconButton(onClick = onConfirm) {
            Icon(Icons.Default.Check, "确认", tint = Color(0xFF43A047))
        }
        IconButton(onClick = onReject) {
            Icon(Icons.Default.Close, "忽略", tint = Color(0xFF9E9E9E))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfirmDialog(
    p: PendingTransaction,
    categories: List<Category>,
    accounts: List<com.ledger.app.data.entity.Account>,
    onDismiss: () -> Unit,
    onConfirm: (Long?, Long?) -> Unit
) {
    var selectedCat by remember {
        mutableStateOf(categories.firstOrNull { it.name == "其他" }?.id)
    }
    var selectedAcc by remember { mutableStateOf(accounts.firstOrNull()?.id) }

    val tops = categories.filter { it.parentId == null && it.type == "expense" }
    val subs = categories.filter { it.parentId == selectedCat }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认入账") },
        text = {
            Column {
                Text("金额：¥${Format.money(p.parsedAmount ?: 0.0)}")
                Text("商户：${p.parsedMerchant ?: "未知"}")
                Spacer(Modifier.height(12.dp))

                Text("分类", fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tops.forEach { c ->
                        FilterChip(
                            selected = selectedCat == c.id,
                            onClick = { selectedCat = c.id },
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
                                selected = selectedCat == c.id,
                                onClick = { selectedCat = c.id },
                                label = { Text(c.name) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("账户", fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    accounts.forEach { a ->
                        FilterChip(
                            selected = selectedAcc == a.id,
                            onClick = { selectedAcc = a.id },
                            label = { Text(a.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedCat, selectedAcc) }) {
                Text("确认入账")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}