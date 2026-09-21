package com.ledger.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ledger.app.ui.components.LedgerCard
import com.ledger.app.ui.components.ScreenHeader
import com.ledger.app.util.Format
import com.ledger.app.util.QianjiRecord

@Composable
fun ImportPreviewScreen(
    nav: NavController,
    records: List<QianjiRecord>,
    onConfirm: () -> Unit
) {
    val preview = records.take(30)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenHeader(
            title = "导入预览",
            subtitle = "确认数据无误后再写入",
            onBack = { nav.popBackStack() }
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
        ) {
            item(key = "summary") {
                LedgerCard(
                    modifier = Modifier.padding(bottom = 10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    border = null
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(Modifier.width(13.dp))
                        Column {
                            Text("共解析 ${records.size} 条记录", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(3.dp))
                            Text("当前展示前 ${preview.size} 条", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f))
                        }
                    }
                }
            }
            itemsIndexed(preview, key = { index, record -> "${record.key}_$index" }) { _, record ->
                LedgerCard(
                    modifier = Modifier.padding(vertical = 4.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(record.category, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "${record.date} · ${if (record.type == "收入") "收入" else "支出"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = (if (record.type == "收入" || record.type == "退款" || record.type == "报销") "+" else "-") + "¥" + Format.money(record.money),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (record.type == "收入" || record.type == "退款" || record.type == "报销") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            if (records.size > preview.size) {
                item(key = "more") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            "还有 ${records.size - preview.size} 条将在确认后一并导入",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Button(
            onClick = onConfirm,
            enabled = records.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .height(52.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text("确认导入 ${records.size} 条", style = MaterialTheme.typography.labelLarge)
        }
    }
}