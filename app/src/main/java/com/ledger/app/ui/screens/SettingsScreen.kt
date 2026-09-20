package com.ledger.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ledger.app.util.BackupManager
import com.ledger.app.util.Format
import com.ledger.app.util.QianjiImporter
import com.ledger.app.vm.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(nav: NavController, vm: MainViewModel) {

    val ctx = LocalContext.current
    val scope = CoroutineScope(Dispatchers.Main)

    val monthlyBudget by vm.monthlyBudget.collectAsStateWithLifecycle(initialValue = 0.0)
    var showBudgetDialog by remember { mutableStateOf(false) }

    fun toast(msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    val pickJson = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }
            if (text.isNullOrBlank()) { toast("读取失败"); return@launch }
            runCatching {
                ImportHolder.records = QianjiImporter.parse(text)
                nav.navigate("import_preview")
            }.onFailure { toast("解析失败：${it.message}") }
        }
    }

    val exportDb = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            BackupManager.exportDatabase(ctx, uri)
                .onSuccess { toast("导出成功") }
                .onFailure { toast("导出失败：${it.message}") }
        }
    }

    val importDb = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            BackupManager.importDatabase(ctx, uri)
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
                (ctx.applicationContext as com.ledger.app.LedgerApp).db,
                uri, ctx.contentResolver
            ).onSuccess { toast("已导出 $it 条") }
                .onFailure { toast("导出失败：${it.message}") }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        Section("预算")
        SettingItem(
            "月度预算",
            if (monthlyBudget > 0) "¥${Format.money(monthlyBudget)} / 月" else "未设置"
        ) { showBudgetDialog = true }

        Spacer(Modifier.height(12.dp))
        Section("记账设置")
        SettingItem("分类管理", "增删改一级/二级分类") { nav.navigate("category_manage") }
        SettingItem("账户管理", "增删改账户") { nav.navigate("account_manage") }

        Spacer(Modifier.height(12.dp))
        Section("自动记账")
        SettingItem("通知监听", "系统设置 → 通知使用权") {
            ctx.startActivity(android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }
        SettingItem("无障碍服务", "系统设置 → 无障碍") {
            ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        Spacer(Modifier.height(12.dp))
        Section("数据管理")
        SettingItem("导入钱迹数据", "从 JSON 文件导入") {
            pickJson.launch(arrayOf("application/json", "text/plain", "*/*"))
        }
        SettingItem("导出数据库", "导出 .db 备份") {
            exportDb.launch(BackupManager.defaultDbFileName())
        }
        SettingItem("导入数据库", "从 .db 恢复") {
            importDb.launch(arrayOf("*/*"))
        }
        SettingItem("导出 CSV", "导出可读文本") {
            exportCsv.launch(BackupManager.defaultCsvFileName())
        }

        Spacer(Modifier.height(12.dp))
        Section("关于")
        SettingItem("版本", "1.0") { toast("Ledger 1.0") }
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
                    Text("设置后，账单页会显示今日可花、累计结余等。",
                        fontSize = 12.dp.value.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = temp,
                        onValueChange = { temp = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("每月预算（元）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("留空或填 0 表示关闭预算",
                        fontSize = 11.dp.value.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = temp.toDoubleOrNull() ?: 0.0
                    vm.setMonthlyBudget(v)
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
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingItem(title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider()
}