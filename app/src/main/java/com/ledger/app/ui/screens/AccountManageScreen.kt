package com.ledger.app.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ledger.app.data.entity.Account
import com.ledger.app.ui.components.EmptyState
import com.ledger.app.ui.components.LedgerCard
import com.ledger.app.ui.components.ScreenHeader
import com.ledger.app.util.Format
import com.ledger.app.vm.MainViewModel

private val AccountTypes = listOf(
    "cash" to "现金",
    "alipay" to "支付宝",
    "wechat" to "微信",
    "bank" to "银行卡",
    "credit" to "信用卡",
    "other" to "其他"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountManageScreen(nav: NavController, vm: MainViewModel) {
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Account?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Account?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = "账户管理",
                subtitle = "统一管理资金账户",
                onBack = { nav.popBackStack() }
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                if (accounts.isEmpty()) {
                    item {
                        EmptyState(
                            emoji = "👛",
                            title = "还没有账户",
                            subtitle = "添加现金、支付宝、微信或银行卡账户"
                        )
                    }
                } else {
                    items(accounts, key = { it.id }) { account ->
                        AccountCard(
                            account = account,
                            onEdit = { editing = account },
                            onDelete = { deleting = account }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "新增账户")
        }
    }

    if (showAdd) {
        AccountEditDialog(
            title = "新增账户",
            initial = null,
            onDismiss = { showAdd = false },
            onConfirm = { name, type ->
                vm.addAccount(Account(name = name, type = type, sortOrder = accounts.size))
                showAdd = false
            }
        )
    }

    editing?.let { account ->
        AccountEditDialog(
            title = "编辑账户",
            initial = account,
            onDismiss = { editing = null },
            onConfirm = { name, type ->
                vm.updateAccount(account.copy(name = name, type = type))
                editing = null
            }
        )
    }

    deleting?.let { account ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除账户「${account.name}」？") },
            text = { Text("删除后已有账单会显示为无账户。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteAccount(account)
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
private fun AccountCard(account: Account, onEdit: () -> Unit, onDelete: () -> Unit) {
    val icon = if (account.type == "bank" || account.type == "credit") {
        Icons.Default.AccountBalance
    } else {
        Icons.Default.Wallet
    }
    LedgerCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(3.dp))
                Text(
                    accountTypeLabel(account.type),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "¥${Format.money(account.balance)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountEditDialog(
    title: String,
    initial: Account?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "other") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("账户名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Text("账户类型", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AccountTypes.forEach { (value, label) ->
                        FilterChip(
                            selected = type == value,
                            onClick = { type = value },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), type) },
                enabled = name.isNotBlank()
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun accountTypeLabel(type: String): String =
    AccountTypes.firstOrNull { it.first == type }?.second ?: "其他"