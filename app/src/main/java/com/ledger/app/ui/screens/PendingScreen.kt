package com.ledger.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ledger.app.vm.MainViewModel

@Composable
fun SettingsScreen(nav: NavController, vm: MainViewModel) {

    val ctx = LocalContext.current

    fun toast(msg: String) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        Section("记账设置")
        SettingItem("分类管理", "增删改一级/二级分类") { nav.navigate("category_manage") }
        SettingItem("账户管理", "增删改账户") { nav.navigate("account_manage") }

        Spacer(Modifier.height(12.dp))
        Section("自动记账")
        SettingItem("通知监听", "监听微信/支付宝支付通知") { toast("开发中") }
        SettingItem("无障碍服务", "读取支付成功页面") { toast("开发中") }
        SettingItem("规则管理", "解析规则模板") { toast("开发中") }

        Spacer(Modifier.height(12.dp))
        Section("数据管理")
        SettingItem("导入钱迹数据", "从 JSON 文件导入") { toast("开发中") }
        SettingItem("导出数据库", "导出 .db 文件") { toast("开发中") }
        SettingItem("导入数据库", "从 .db 文件恢复") { toast("开发中") }

        Spacer(Modifier.height(12.dp))
        Section("关于")
        SettingItem("版本", "1.0") { toast("Ledger 1.0") }
    }
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