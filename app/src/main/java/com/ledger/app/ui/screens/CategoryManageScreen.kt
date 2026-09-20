package com.ledger.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.ledger.app.data.entity.Category
import com.ledger.app.vm.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManageScreen(nav: NavController, vm: MainViewModel) {

    val categories by vm.categories.collectAsStateWithLifecycle(initialValue = emptyList())

    var tab by remember { mutableStateOf(0) }
    val type = if (tab == 0) "expense" else "income"

    val tops = categories.filter { it.parentId == null && it.type == type }
        .sortedBy { it.sortOrder }
    val childrenByParent = categories
        .filter { it.parentId != null }
        .groupBy { it.parentId }

    var editing by remember { mutableStateOf<Category?>(null) }
    var addingParent by remember { mutableStateOf<Long?>(null) }
    var showAddTop by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分类管理") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (tops.isEmpty()) showAddTop = true
                else showAddTop = true
            }) {
                Icon(Icons.Default.Add, "新增")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("支出") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("收入") })
            }

            LazyColumn(Modifier.fillMaxSize()) {
                tops.forEach { top ->
                    item(key = "top_${top.id}") {
                        ListItem(
                            headlineContent = {
                                Text("${top.icon ?: ""} ${top.name}")
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { addingParent = top.id }) {
                                        Icon(Icons.Default.Add, "添加二级")
                                    }
                                    IconButton(onClick = { editing = top }) {
                                        Icon(Icons.Default.Edit, "编辑")
                                    }
                                    IconButton(onClick = { deleting = top }) {
                                        Icon(Icons.Default.Delete, "删除")
                                    }
                                }
                            }
                        )
                    }
                    val subs = childrenByParent[top.id].orEmpty().sortedBy { it.sortOrder }
                    items(subs, key = { it.id }) { sub ->
                        ListItem(
                            headlineContent = {
                                Text("      ${sub.name}", style = MaterialTheme.typography.bodyMedium)
                            },
                            modifier = Modifier.padding(start = 16.dp),
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { editing = sub }) {
                                        Icon(Icons.Default.Edit, "编辑")
                                    }
                                    IconButton(onClick = { deleting = sub }) {
                                        Icon(Icons.Default.Delete, "删除")
                                    }
                                }
                            }
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // 新增一级分类
    if (showAddTop) {
        CategoryEditDialog(
            title = "新增一级分类",
            initial = null,
            onDismiss = { showAddTop = false },
            onConfirm = { name, icon ->
                vm.addCategory(
                    Category(
                        name = name,
                        parentId = null,
                        type = type,
                        icon = icon.ifBlank { null },
                        sortOrder = tops.size
                    )
                )
                showAddTop = false
            }
        )
    }

    // 新增二级
    addingParent?.let { pid ->
        val parent = tops.firstOrNull { it.id == pid }
        CategoryEditDialog(
            title = "新增二级（${parent?.name ?: ""}）",
            initial = null,
            showIcon = false,
            onDismiss = { addingParent = null },
            onConfirm = { name, _ ->
                vm.addCategory(
                    Category(
                        name = name,
                        parentId = pid,
                        type = type,
                        sortOrder = childrenByParent[pid].orEmpty().size
                    )
                )
                addingParent = null
            }
        )
    }

    // 编辑
    editing?.let { cat ->
        CategoryEditDialog(
            title = "编辑分类",
            initial = cat,
            showIcon = cat.parentId == null,
            onDismiss = { editing = null },
            onConfirm = { name, icon ->
                vm.updateCategory(
                    cat.copy(name = name, icon = icon.ifBlank { null })
                )
                editing = null
            }
        )
    }

    // 删除确认
    deleting?.let { cat ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除分类「${cat.name}」？") },
            text = {
                val subCount = childrenByParent[cat.id].orEmpty().size
                if (subCount > 0) Text("该分类下有 $subCount 个二级分类，也会一并删除。")
                else Text("删除后不影响已有账单，账单会显示为未分类。")
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteCategory(cat)
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
private fun CategoryEditDialog(
    title: String,
    initial: Category?,
    showIcon: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (name: String, icon: String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var icon by remember { mutableStateOf(initial?.icon ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showIcon) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = icon,
                        onValueChange = { icon = it },
                        label = { Text("图标（emoji，可留空）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), icon.trim()) },
                enabled = name.isNotBlank()
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}