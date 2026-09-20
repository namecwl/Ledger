package com.ledger.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ledger.app.data.entity.Account
import com.ledger.app.vm.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManageScreen(nav: NavController, vm: MainViewModel) {

    val accounts by vm.accounts.collectAsStateWithLifecycle(initialValue = emptyList())

    var editing by remember { mutableStateOf<Account?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Account?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("账户管理") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, "新增")
            }
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            items(accounts, key = { it.id }) { acc ->
                ListItem(
                    headlineContent = { Text(acc.name) },
                    supportingContent = { Text("类型：${acc.type}") },
                    trailingContent = {
                        Column {
                            IconButton(onClick = { editing = acc }) {
                                Icon(Icons.Default.Edit, "编辑")
                            }
                            IconButton(onClick = { deleting = acc }) {
                                Icon(Icons.Default.Delete, "删除")
                            }
                        }
                    }
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAdd) {
        AccountEditDialog(
            title = "新增账户",
            initial = null,
            onDismiss = { showAdd = false },
            onConfirm = { name, type ->
                vm.addAccount(
                    Account(
                        name = name,
                        type = type,
                        sortOrder = accounts.size
                    )
                )
                showAdd = false
            }
        )
    }

    editing?.let { acc ->
        AccountEditDialog(
            title = "编辑账户",
            initial = acc,
            onDismiss = { editing = null },
            onConfirm = { name, type ->
                vm.updateAccount(acc.copy(name = name, type = type))
                editing = null
            }
        )
    }

    deleting?.let { acc ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除账户「${acc.name}」？") },
            text = { Text("删除后已有账单会显示为无账户。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteAccount(acc)
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
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("类型（cash/alipay/wechat/bank/credit/other）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), type.trim()) },
                enabled = name.isNotBlank()
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}