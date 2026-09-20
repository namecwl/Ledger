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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ledger.app.util.QianjiRecord
import com.ledger.app.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewScreen(
    nav: NavController,
    records: List<QianjiRecord>,
    onConfirm: () -> Unit
) {
    val preview = records.take(20)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导入预览") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(
                "共解析 ${records.size} 条记录",
                modifier = Modifier.padding(16.dp)
            )
            HorizontalDivider()
            LazyColumn(Modifier.weight(1f)) {
                items(preview) { r ->
                    ListItem(
                        headlineContent = { Text("${r.category} · ¥${Format.money(r.money)}") },
                        supportingContent = { Text("${r.date} · ${r.type}") }
                    )
                    HorizontalDivider()
                }
                if (records.size > preview.size) {
                    item {
                        Text(
                            "…… 还有 ${records.size - preview.size} 条",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("确认导入")
            }
        }
    }
}