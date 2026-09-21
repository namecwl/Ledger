package com.ledger.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.app.data.entity.Category
import com.ledger.app.data.entity.PendingTransaction
import com.ledger.app.ui.components.EmptyState
import com.ledger.app.ui.components.LedgerCard
import com.ledger.app.ui.components.ScreenHeader
import com.ledger.app.ui.theme.AmountColors
import com.ledger.app.util.Format
import com.ledger.app.vm.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PendingScreen(vm: MainViewModel) {
    val pendingTransactions by vm.pendingList.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    var confirming by remember { mutableStateOf<PendingTransaction?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item(key = "header", contentType = "screen_header") {
            ScreenHeader(
                title = "待确认",
                subtitle = if (pendingTransactions.isEmpty()) "自动记账结果会出现在这里" else "${pendingTransactions.size} 笔记录等待确认"
            )
        }
        if (pendingTransactions.isEmpty()) {
            item(key = "empty", contentType = "empty") {
                EmptyState(
                    emoji = "🔔",
                    title = "暂无待确认账单",
                    subtitle = "开启通知监听或无障碍服务后，支付消息会在确认前自动保留"
                )
            }
        } else {
            items(pendingTransactions, key = { it.id }, contentType = { "pending" }) { pending ->
                PendingItem(
                    pending = pending,
                    onConfirm = { confirming = pending },
                    onReject = { vm.rejectPending(pending) }
                )
            }
        }
    }

    confirming?.let { pending ->
        ConfirmDialog(
            pending = pending,
            categories = categories,
            accounts = accounts,
            onDismiss = { confirming = null },
            onConfirm = { categoryId, accountId ->
                vm.confirmPending(pending, categoryId, accountId)
                confirming = null
            }
        )
    }
}

@Composable
private fun PendingItem(
    pending: PendingTransaction,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    val confidenceColor = when {
        pending.confidence >= 0.8 -> AmountColors.Income
        pending.confidence >= 0.5 -> AmountColors.Warning
        else -> AmountColors.Expense
    }
    val sourceLabel = when (pending.source) {
        "notification" -> "通知"
        "accessibility" -> "无障碍"
        else -> "自动识别"
    }

    LedgerCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(confidenceColor.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = confidenceColor,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    pending.parsedMerchant ?: "未知商户",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = confidenceColor.copy(alpha = 0.11f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            "$sourceLabel · ${(pending.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = confidenceColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(7.dp))
                    Text(
                        Format.timeOf(pending.parsedDate ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "¥${Format.money(pending.parsedAmount ?: 0.0)}",
                    color = AmountColors.Expense,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    IconButton(onClick = onReject, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "忽略", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onConfirm, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Check, contentDescription = "确认", tint = AmountColors.Income, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfirmDialog(
    pending: PendingTransaction,
    categories: List<Category>,
    accounts: List<com.ledger.app.data.entity.Account>,
    onDismiss: () -> Unit,
    onConfirm: (Long?, Long?) -> Unit
) {
    var selectedCategory by remember {
        mutableStateOf(categories.firstOrNull { it.name == "其他" }?.id)
    }
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    val tops = remember(categories) {
        categories.filter { it.parentId == null && it.type == "expense" }
    }
    val subs = remember(categories, selectedCategory) {
        categories.filter { it.parentId == selectedCategory }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认入账") },
        text = {
            Column {
                LedgerCard(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = null,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Text(pending.parsedMerchant ?: "未知商户", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "¥${Format.money(pending.parsedAmount ?: 0.0)}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AmountColors.Expense,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(14.dp))
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
                Spacer(Modifier.height(14.dp))
                Text("账户", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    accounts.forEach { account ->
                        FilterChip(
                            selected = selectedAccount == account.id,
                            onClick = { selectedAccount = account.id },
                            label = { Text(account.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedCategory, selectedAccount) }) {
                Text("确认入账")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}