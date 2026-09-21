package com.ledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ledger.app.data.entity.Category
import com.ledger.app.ui.components.LedgerCard
import com.ledger.app.ui.components.ScreenHeader
import com.ledger.app.vm.MainViewModel

@Composable
fun CategoryManageScreen(nav: NavController, vm: MainViewModel) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(0) }
    val type = if (tab == 0) "expense" else "income"

    val tops = remember(categories, type) {
        categories.filter { it.parentId == null && it.type == type }.sortedBy { it.sortOrder }
    }
    val childrenByParent = remember(categories) {
        categories.filter { it.parentId != null }.groupBy { it.parentId }
    }

    var editing by remember { mutableStateOf<Category?>(null) }
    var addingParent by remember { mutableStateOf<Long?>(null) }
    var showAddTop by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Category?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = "分类管理",
                subtitle = "整理你的记账习惯",
                onBack = { nav.popBackStack() }
            )
            CategoryTabs(selected = tab, onSelect = { tab = it })
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(tops, key = { it.id }) { top ->
                    val children = childrenByParent[top.id].orEmpty().sortedBy { it.sortOrder }
                    CategoryTopCard(
                        category = top,
                        children = children,
                        onAddChild = { addingParent = top.id },
                        onEdit = { editing = top },
                        onDelete = { deleting = top },
                        onEditChild = { editing = it },
                        onDeleteChild = { deleting = it }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddTop = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "新增一级分类")
        }
    }

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

    addingParent?.let { parentId ->
        val parent = tops.firstOrNull { it.id == parentId }
        CategoryEditDialog(
            title = "新增二级 · ${parent?.name.orEmpty()}",
            initial = null,
            showIcon = false,
            onDismiss = { addingParent = null },
            onConfirm = { name, _ ->
                vm.addCategory(
                    Category(
                        name = name,
                        parentId = parentId,
                        type = type,
                        sortOrder = childrenByParent[parentId].orEmpty().size
                    )
                )
                addingParent = null
            }
        )
    }

    editing?.let { category ->
        CategoryEditDialog(
            title = "编辑分类",
            initial = category,
            showIcon = category.parentId == null,
            onDismiss = { editing = null },
            onConfirm = { name, icon ->
                vm.updateCategory(category.copy(name = name, icon = icon.ifBlank { null }))
                editing = null
            }
        )
    }

    deleting?.let { category ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除分类「${category.name}」？") },
            text = {
                val childCount = childrenByParent[category.id].orEmpty().size
                if (childCount > 0) {
                    Text("该分类下有 $childCount 个二级分类，也会一并删除。已有账单会显示为未分类。")
                } else {
                    Text("删除后已有账单会显示为未分类。")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteCategory(category)
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
private fun CategoryTabs(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        listOf("支出", "收入").forEachIndexed { index, label ->
            val active = selected == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (active) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryTopCard(
    category: Category,
    children: List<Category>,
    onAddChild: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEditChild: (Category) -> Unit,
    onDeleteChild: (Category) -> Unit
) {
    LedgerCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 10.dp, end = 6.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(category.icon ?: "📝", fontSize = 20.sp)
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleSmall)
                Text("${children.size} 个子分类", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onAddChild) { Icon(Icons.Default.Add, contentDescription = "添加子分类") }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "编辑") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
        }

        children.forEach { child ->
            HorizontalDivider(modifier = Modifier.padding(start = 66.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 67.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(child.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { onEditChild(child) }) { Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(19.dp)) }
                IconButton(onClick = { onDeleteChild(child) }) { Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(19.dp)) }
            }
        }
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