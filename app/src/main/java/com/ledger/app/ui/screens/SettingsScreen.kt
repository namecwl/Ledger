package com.ledger.app.ui.screens

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
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("记账设置", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        SettingItem("分类管理", "增删改一级/二级分类")
        SettingItem("账户管理", "增删改账户")

        Spacer(Modifier.height(20.dp))
        Text("自动记账", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        SettingItem("通知监听", "监听微信/支付宝支付通知")
        SettingItem("无障碍服务", "读取支付成功页面")
        SettingItem("规则管理", "解析规则模板")

        Spacer(Modifier.height(20.dp))
        Text("数据管理", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        SettingItem("导入钱迹数据", "从 JSON 文件导入")
        SettingItem("导出数据库", "导出 .db 文件")
        SettingItem("导入数据库", "从 .db 文件恢复")

        Spacer(Modifier.height(20.dp))
        Text("关于", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        SettingItem("版本", "1.0")
    }
}

@Composable
private fun SettingItem(title: String, subtitle: String) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) }
    )
    HorizontalDivider()
}