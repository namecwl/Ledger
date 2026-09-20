package com.ledger.app.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.app.data.entity.Category
import com.ledger.app.data.entity.Transaction
import com.ledger.app.ui.theme.AmountColors
import com.ledger.app.util.Format
import com.ledger.app.vm.MainViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordScreen(vm: MainViewModel) {

    val ctx = LocalContext.current
    val categories by vm.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val accounts by vm.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val todayBudget by vm.todayBudget.collectAsStateWithLifecycle()

    var type by remember { mutableStateOf("expense") }
    var amountText by remember { mutableStateOf("") }
    var selectedTop by remember { mutableStateOf<Long?>(null) }
    var selectedSub by remember { mutableStateOf<Long?>(null) }
    var selectedAccount by remember { mutableStateOf<Long?>(null) }
    var remark by remember { mutableStateOf("") }
    var pickedDateTime by remember { mutableStateOf(LocalDateTime.now()) }

    var showRemarkDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val tops = remember(categories, type) {
        categories.filter { it.parentId == null && it.type == type }
    }
    val subs = remember(categories, selectedTop) {
        categories.filter { it.parentId == selectedTop }
    }

    LaunchedEffect(tops) {
        if (tops.none { it.id == selectedTop }) {
            selectedTop = tops.firstOrNull()?.id
            selectedSub = null
        }
    }
    LaunchedEffect(accounts) {
        if (selectedAccount == null) selectedAccount = accounts.firstOrNull()?.id
    }

    val isExpense = type == "expense"
    val amountColor by animateColorAsState(
        if (isExpense) AmountColors.Expense else AmountColors.Income,
        label = "amtColor"
    )

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {

            // 顶部类型切换 + 今日可花
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TypeSwitch(current = type, onSelect = {
                    type = it
                    selectedSub = null
                })
                if (todayBudget != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("今日可花", fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "¥${Format.money(todayBudget!!.todayAllowance.coerceAtLeast(0.0))}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 大金额
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.End
            ) {
                Text("¥", fontSize = 22.sp, color = amountColor,
                    modifier = Modifier.padding(bottom = 6.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (amountText.isEmpty()) "0.00" else formatAmount(amountText),
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (amountText.isEmpty())
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    else amountColor
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 分类网格
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 14.dp)
            ) {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    tops.forEach { cat ->
                        CategoryCell(
                            category = cat,
                            selected = selectedTop == cat.id,
                            onClick = {
                                selectedTop = cat.id
                                selectedSub = null
                            }
                        )
                    }
                }

                if (subs.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("二级分类", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        subs.forEach { sub ->
                            FilterChip(
                                selected = selectedSub == sub.id,
                                onClick = { selectedSub = sub.id },
                                label = { Text(sub.name) }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 底部信息栏
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showRemarkDialog = true }) {
                    Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(remark.ifBlank { "备注" }, maxLines = 1, fontSize = 13.sp)
                }
                TextButton(onClick = {
                    pickDateTime(ctx, pickedDateTime) { pickedDateTime = it }
                }) {
                    Text(
                        pickedDateTime.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")),
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showAccountDialog = true }) {
                    Text(
                        accounts.firstOrNull { it.id == selectedAccount }?.name ?: "账户",
                        fontSize = 13.sp
                    )
                }
            }

            // 键盘
            Keypad(
                accentColor = amountColor,
                onDigit = { d ->
                    if (amountText.length < 10) {
                        amountText = if (amountText == "0") d else amountText + d
                    }
                },
                onDot = {
                    if (!amountText.contains(".")) {
                        amountText = if (amountText.isEmpty()) "0." else "$amountText."
                    }
                },
                onBackspace = {
                    if (amountText.isNotEmpty()) amountText = amountText.dropLast(1)
                },
                onSave = {
                    val amount = amountText.toDoubleOrNull() ?: return@Keypad
                    if (amount <= 0) return@Keypad
                    val iso = pickedDateTime.format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                    )
                    vm.saveTransaction(
                        Transaction(
                            type = type,
                            amount = amount,
                            categoryId = selectedSub ?: selectedTop,
                            accountId = selectedAccount,
                            date = iso,
                            remark = remark.ifBlank { null },
                            createdAt = iso,
                            updatedAt = iso
                        )
                    )
                    amountText = ""
                    remark = ""
                    pickedDateTime = LocalDateTime.now()
                    scope.launch { snackbar.showSnackbar("已记账 ¥${Format.money(amount)}") }
                },
                canSave = (amountText.toDoubleOrNull() ?: 0.0) > 0
            )
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 270.dp)
        )
    }

    if (showRemarkDialog) {
        var temp by remember { mutableStateOf(remark) }
        AlertDialog(
            onDismissRequest = { showRemarkDialog = false },
            title = { Text("备注") },
            text = {
                OutlinedTextField(
                    value = temp,
                    onValueChange = { temp = it },
                    singleLine = true,
                    placeholder = { Text("添加备注…") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    remark = temp
                    showRemarkDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showRemarkDialog = false }) { Text("取消") }
            }
        )
    }

    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = { Text("选择账户") },
            text = {
                Column {
                    accounts.forEach { acc ->
                        ListItem(
                            headlineContent = { Text(acc.name) },
                            modifier = Modifier.clickable {
                                selectedAccount = acc.id
                                showAccountDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountDialog = false }) { Text("关闭") }
            }
        )
    }
}

private fun pickDateTime(
    ctx: android.content.Context,
    initial: LocalDateTime,
    onPicked: (LocalDateTime) -> Unit
) {
    DatePickerDialog(
        ctx,
        { _, y, m, d ->
            TimePickerDialog(
                ctx,
                { _, h, min -> onPicked(LocalDateTime.of(y, m + 1, d, h, min)) },
                initial.hour, initial.minute, true
            ).show()
        },
        initial.year, initial.monthValue - 1, initial.dayOfMonth
    ).show()
}

private fun formatAmount(raw: String): String {
    val dot = raw.indexOf('.')
    if (dot < 0) return raw
    val intPart = raw.substring(0, dot)
    val decPart = raw.substring(dot)
    if (intPart.isEmpty()) return raw
    val grouped = intPart.reversed().chunked(3).joinToString(",").reversed()
    return grouped + decPart
}

@Composable
private fun TypeSwitch(current: String, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        listOf("expense" to "支出", "income" to "收入").forEach { (key, label) ->
            val selected = current == key
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.surface else Color.Transparent
                    )
                    .clickable { onSelect(key) }
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryCell(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(category.icon ?: "📝", fontSize = 22.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            category.name,
            fontSize = 12.sp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun Keypad(
    accentColor: Color,
    onDigit: (String) -> Unit,
    onDot: () -> Unit,
    onBackspace: () -> Unit,
    onSave: () -> Unit,
    canSave: Boolean
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(6.dp)
    ) {
        Column(Modifier.weight(1f)) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(".", "0", "⌫")
            )
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { key ->
                        KeyButton(
                            key = key,
                            onClick = {
                                when (key) {
                                    "⌫" -> onBackspace()
                                    "." -> onDot()
                                    else -> onDigit(key)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier
                .width(92.dp)
                .height(202.dp)
                .padding(4.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
            Text("保存", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun KeyButton(
    key: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .height(50.dp)
            .padding(3.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        if (key == "⌫") {
            Icon(Icons.Default.Backspace, null, modifier = Modifier.size(20.dp))
        } else {
            Text(key, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        }
    }
}