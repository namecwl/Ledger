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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.app.data.entity.Category
import com.ledger.app.data.entity.Transaction
import com.ledger.app.ui.components.LedgerCard
import com.ledger.app.ui.components.ScreenHeader
import com.ledger.app.ui.theme.AmountColors
import com.ledger.app.util.Format
import com.ledger.app.vm.MainViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime

private val KeypadRows = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf(".", "0", "⌫")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val categories by vm.categories.collectAsStateWithLifecycle()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
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
    val activeTop = selectedTop?.takeIf { id -> tops.any { it.id == id } } ?: tops.firstOrNull()?.id
    val subs = remember(categories, activeTop) {
        categories.filter { it.parentId == activeTop }
    }
    val activeAccount = selectedAccount?.takeIf { id -> accounts.any { it.id == id } }
        ?: accounts.firstOrNull()?.id

    val isExpense = type == "expense"
    val amountColor by animateColorAsState(
        targetValue = if (isExpense) AmountColors.Expense else AmountColors.Income,
        label = "amount_color"
    )
    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val canSave = parsedAmount > 0.0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = "记一笔",
                subtitle = "简单一点，坚持更久",
                trailing = {
                    if (todayBudget != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    "今日可花",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                                )
                                Text(
                                    "¥${Format.money(todayBudget!!.todayAllowance.coerceAtLeast(0.0))}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            )

            LedgerCard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 15.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TypeSwitch(current = type, onSelect = {
                        type = it
                        selectedSub = null
                    })
                    Spacer(Modifier.weight(1f))
                    Text(
                        "人民币",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        "¥",
                        fontSize = 22.sp,
                        color = amountColor,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (amountText.isEmpty()) "0.00" else formatAmount(amountText),
                        fontSize = 44.sp,
                        lineHeight = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (amountText.isEmpty()) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
                        } else {
                            amountColor
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    "选择分类",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tops.forEach { category ->
                        CategoryCell(
                            category = category,
                            selected = activeTop == category.id,
                            onClick = {
                                selectedTop = category.id
                                selectedSub = null
                            }
                        )
                    }
                }

                if (subs.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "子分类",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(7.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        subs.forEach { subcategory ->
                            FilterChip(
                                selected = selectedSub == subcategory.id,
                                onClick = { selectedSub = subcategory.id },
                                label = { Text(subcategory.name) }
                            )
                        }
                    }
                }
            }

            ActionBar(
                remark = remark,
                dateTime = pickedDateTime,
                accountName = accounts.firstOrNull { it.id == activeAccount }?.name ?: "账户",
                onRemark = { showRemarkDialog = true },
                onDateTime = { pickDateTime(context, pickedDateTime) { pickedDateTime = it } },
                onAccount = { showAccountDialog = true }
            )

            Keypad(
                accentColor = amountColor,
                onDigit = { digit ->
                    if (amountText.length < 10) {
                        amountText = if (amountText == "0") digit else amountText + digit
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
                    if (!canSave) return@Keypad
                    val iso = Format.iso(pickedDateTime)
                    vm.saveTransaction(
                        Transaction(
                            type = type,
                            amount = parsedAmount,
                            categoryId = selectedSub ?: activeTop,
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
                    scope.launch { snackbar.showSnackbar("已记下 ¥${Format.money(parsedAmount)}") }
                },
                canSave = canSave
            )
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 220.dp)
        )
    }

    if (showRemarkDialog) {
        var temp by remember { mutableStateOf(remark) }
        AlertDialog(
            onDismissRequest = { showRemarkDialog = false },
            title = { Text("添加备注") },
            text = {
                OutlinedTextField(
                    value = temp,
                    onValueChange = { temp = it },
                    singleLine = true,
                    placeholder = { Text("例如：和朋友午餐") },
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
                    accounts.forEach { account ->
                        val selected = selectedAccount == account.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedAccount = account.id
                                    showAccountDialog = false
                                }
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Wallet, null, modifier = Modifier.size(17.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(account.name, modifier = Modifier.weight(1f))
                            if (selected) Text("已选择", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
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
    context: android.content.Context,
    initial: LocalDateTime,
    onPicked: (LocalDateTime) -> Unit
) {
    DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute -> onPicked(LocalDateTime.of(year, month + 1, day, hour, minute)) },
                initial.hour,
                initial.minute,
                true
            ).show()
        },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth
    ).show()
}

private fun formatAmount(raw: String): String {
    val dot = raw.indexOf('.')
    if (dot < 0) return raw.reversed().chunked(3).joinToString(",").reversed()
    val integerPart = raw.substring(0, dot)
    val decimalPart = raw.substring(dot)
    if (integerPart.isEmpty()) return raw
    val grouped = integerPart.reversed().chunked(3).joinToString(",").reversed()
    return grouped + decimalPart
}

@Composable
private fun TypeSwitch(current: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp)
    ) {
        listOf("expense" to "支出", "income" to "收入").forEach { (key, label) ->
            val selected = current == key
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelect(key) }
                    .padding(horizontal = 20.dp, vertical = 7.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryCell(category: Category, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.66f)
                else Color.Transparent
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(category.icon ?: "📝", fontSize = 20.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            category.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ActionBar(
    remark: String,
    dateTime: LocalDateTime,
    accountName: String,
    onRemark: () -> Unit,
    onDateTime: () -> Unit,
    onAccount: () -> Unit
) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onRemark, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Edit, null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(remark.ifBlank { "备注" }, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
        }
        TextButton(onClick = onDateTime, modifier = Modifier.weight(1.15f)) {
            Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(Format.monthDayTime(dateTime), maxLines = 1, fontSize = 12.sp)
        }
        TextButton(onClick = onAccount, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Wallet, null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(accountName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
        }
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                KeypadRows.forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
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
                    .width(88.dp)
                    .height(192.dp)
                    .padding(4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("保存", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun KeyButton(key: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(48.dp)
            .padding(3.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
    ) {
        if (key == "⌫") {
            Icon(Icons.Default.Backspace, null, modifier = Modifier.size(20.dp))
        } else {
            Text(key, fontSize = 19.sp, fontWeight = FontWeight.Medium)
        }
    }
}