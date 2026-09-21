package com.ledger.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ledger.app.BuildConfig
import com.ledger.app.data.entity.PendingTransaction
import com.ledger.app.service.ServiceStatus
import com.ledger.app.ui.components.LedgerCard
import com.ledger.app.ui.components.LedgerIcon
import com.ledger.app.ui.components.ScreenHeader
import com.ledger.app.ui.components.SectionTitle
import com.ledger.app.util.BackupManager
import com.ledger.app.util.Format
import com.ledger.app.util.QianjiImporter
import com.ledger.app.vm.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(nav: NavController, vm: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val monthlyBudget by vm.monthlyBudget.collectAsStateWithLifecycle()
    val pendingCount by vm.pendingCount.collectAsStateWithLifecycle()
    var showBudgetDialog by remember { mutableStateOf(false) }

    // 每次回到设置页都重新读取三项授权的实时状态
    var statusTick by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) statusTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val notificationOn = remember(statusTick) { ServiceStatus.notificationListenerEnabled(context) }
    val accessibilityOn = remember(statusTick) { ServiceStatus.accessibilityEnabled(context) }
    val batteryOn = remember(statusTick) { ServiceStatus.batteryOptimizationIgnored(context) }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    val pickJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }
            if (text.isNullOrBlank()) {
                toast("读取失败")
                return@launch
            }
            runCatching {
                val records = withContext(Dispatchers.Default) { QianjiImporter.parse(text) }
                ImportHolder.records = records
                nav.navigate("import_preview")
            }.onFailure { toast("解析失败：${it.message}") }
        }
    }

    val exportDb = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            BackupManager.exportDatabase(context, uri)
                .onSuccess { toast("导出成功") }
                .onFailure { toast("导出失败：${it.message}") }
        }
    }

    val importDb = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            BackupManager.importDatabase(context, uri)
                .onSuccess {
                    toast("导入成功，请重启 App")
                    kotlin.system.exitProcess(0)
                }
                .onFailure { toast("导入失败：${it.message}") }
        }
    }

    val exportCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            BackupManager.exportCsv(
                (context.applicationContext as com.ledger.app.LedgerApp).db,
                uri,
                context.contentResolver
            ).onSuccess { toast("已导出 $it 条") }
                .onFailure { toast("导出失败：${it.message}") }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item(key = "header") {
            ScreenHeader(title = "设置", subtitle = "让记账更顺手")
        }

        item(key = "budget_title") { SectionTitle("预算") }
        item(key = "budget") {
            LedgerCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                SettingRow(
                    icon = Icons.Default.Edit,
                    title = "月度预算",
                    subtitle = if (monthlyBudget > 0) "¥${Format.money(monthlyBudget)} / 月" else "未设置",
                    onClick = { showBudgetDialog = true }
                )
            }
        }

        item(key = "ledger_title") { SectionTitle("记账设置") }
        item(key = "ledger_group") {
            LedgerCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                SettingRow(
                    icon = Icons.Default.Settings,
                    title = "分类管理",
                    subtitle = "增删改一级、二级分类",
                    onClick = { nav.navigate("category_manage") }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "账户管理",
                    subtitle = "管理现金、支付宝、银行卡等账户",
                    onClick = { nav.navigate("account_manage") }
                )
            }
        }

        item(key = "auto_title") { SectionTitle("自动记账") }
        item(key = "auto_status") {
            AutoBookkeepingStatusCard(
                notificationOn = notificationOn,
                accessibilityOn = accessibilityOn,
                batteryOn = batteryOn,
                pendingCount = pendingCount,
                onTest = {
                    scope.launch {
                        val now = Format.nowIso()
                        (context.applicationContext as com.ledger.app.LedgerApp).db
                            .pendingDao().insert(
                                PendingTransaction(
                                    source = "test",
                                    rawText = "自动记账链路测试",
                                    parsedAmount = 0.01,
                                    parsedMerchant = "自动记账测试",
                                    parsedDate = now,
                                    parsedType = "expense",
                                    confidence = 0.99,
                                    status = "pending",
                                    createdAt = now
                                )
                            )
                        toast("已生成测试记录，请到「待确认」查看")
                    }
                }
            )
        }
        item(key = "auto_group") {
            LedgerCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                SettingRow(
                    icon = Icons.Default.Notifications,
                    title = "通知监听",
                    subtitle = "读取支付通知并生成待确认账单",
                    onClick = { context.startActivity(android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    icon = Icons.Default.Accessibility,
                    title = "无障碍服务",
                    subtitle = "从支付页面辅助识别账单",
                    onClick = { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    icon = Icons.Default.BatteryFull,
                    title = "后台保活",
                    subtitle = "允许后台运行/忽略电池优化，防止服务被自动关闭",
                    onClick = {
                        val power = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
                        val intent = if (!power.isIgnoringBatteryOptimizations(context.packageName)) {
                            android.content.Intent(
                                android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                        } else {
                            android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        }
                        runCatching { context.startActivity(intent) }.onFailure {
                            runCatching { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS)) }
                        }
                    }
                )
            }
        }

        item(key = "data_title") { SectionTitle("数据管理") }
        item(key = "data_group") {
            LedgerCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                SettingRow(
                    icon = Icons.Default.FileDownload,
                    title = "导入钱迹数据",
                    subtitle = "从 JSON 文件导入",
                    onClick = { pickJson.launch(arrayOf("application/json", "text/plain", "*/*")) }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    icon = Icons.Default.FileUpload,
                    title = "导出数据库",
                    subtitle = "导出 .db 完整备份",
                    onClick = { exportDb.launch(BackupManager.defaultDbFileName()) }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    icon = Icons.Default.Storage,
                    title = "导入数据库",
                    subtitle = "从 .db 文件恢复数据",
                    onClick = { importDb.launch(arrayOf("*/*")) }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    icon = Icons.Default.FileUpload,
                    title = "导出 CSV",
                    subtitle = "导出可读的表格文本",
                    onClick = { exportCsv.launch(BackupManager.defaultCsvFileName()) }
                )
            }
        }

        item(key = "update_title") { SectionTitle("软件更新") }
        item(key = "update_group") {
            LedgerCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                SettingRow(
                    icon = Icons.Default.SystemUpdate,
                    title = "检查更新",
                    subtitle = "当前版本 ${BuildConfig.VERSION_NAME}",
                    onClick = { nav.navigate("update") }
                )
            }
        }

        item(key = "about_title") { SectionTitle("关于") }
        item(key = "about_group") {
            LedgerCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                SettingRow(
                    icon = Icons.Default.Info,
                    title = "版本",
                    subtitle = "Ledger ${BuildConfig.VERSION_NAME}",
                    onClick = { toast("Ledger ${BuildConfig.VERSION_NAME}") }
                )
            }
        }
    }

    if (showBudgetDialog) {
        var temp by remember {
            mutableStateOf(if (monthlyBudget > 0) monthlyBudget.toInt().toString() else "")
        }
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("设置月度预算") },
            text = {
                Column {
                    Text(
                        "设置后，账单页会显示今日可花和本月剩余预算。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = temp,
                        onValueChange = { value -> temp = value.filter { it.isDigit() || it == '.' } },
                        label = { Text("每月预算（元）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "留空或填 0 表示关闭预算",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setMonthlyBudget(temp.toDoubleOrNull() ?: 0.0)
                    showBudgetDialog = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) { Text("取消") }
            }
        )
    }
}

object ImportHolder {
    var records: List<com.ledger.app.util.QianjiRecord> = emptyList()
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LedgerIcon(icon = icon, contentDescription = null, modifier = Modifier.size(42.dp))
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun AutoBookkeepingStatusCard(
    notificationOn: Boolean,
    accessibilityOn: Boolean,
    batteryOn: Boolean,
    pendingCount: Int,
    onTest: () -> Unit
) {
    LedgerCard(
        modifier = Modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("自动记账运行状态", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                "待确认 $pendingCount 笔",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        StatusLine("通知监听", notificationOn)
        StatusLine("无障碍服务", accessibilityOn)
        StatusLine("后台保活", batteryOn)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onTest) { Text("发送测试记录") }
        }
    }
}

@Composable
private fun StatusLine(label: String, enabled: Boolean) {
    val color = if (enabled) {
        Color(0xFF2E9E5B)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.size(7.dp))
        Text(
            if (enabled) "已开启" else "未开启",
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}