package com.ledger.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.app.data.entity.Transaction
import com.ledger.app.vm.MainViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordScreen(vm: MainViewModel) {

    val categories by vm.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val accounts by vm.accounts.collectAsStateWithLifecycle(initialValue = emptyList())

    var type by remember { mutableStateOf("expense") }
    var amountText by remember { mutableStateOf("") }
    var selectedTop by remember { mutableStateOf<Long?>(null) }
    var selectedSub by remember { mutableStateOf<Long?>(null) }
    var selectedAccount by remember { mutableStateOf<Long?>(null) }
    var remark by remember { mutableStateOf("") }

    val tops = remember(categories, type) {
        categories.filter { it.parentId == null && it.type == type }
    }
    val subs = remember(categories, selectedTop) {
        categories.filter { it.parentId == selectedTop }
    }

    LaunchedEffect(tops) {
        if (tops.none { it.id == selectedTop }) {
            selectedTop = tops.firstOrNull()?.id
        }
    }
    LaunchedEffect(subs) {
        if (subs.none { it.id == selectedSub }) {
            selectedSub = subs.firstOrNull()?.id
        }
    }
    LaunchedEffect(accounts) {
        if (selectedAccount == null) selectedAccount = accounts.firstOrNull()?.id
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row {
            listOf("expense" to "支出", "income" to "收入").forEach { (k, label) ->
                FilterChip(
                    selected = type == k,
                    onClick = { type = k },
                    label = { Text(label) }
                )
                Spacer(Modifier.width(8.dp))
            }
        }

        OutlinedTextField(
            value = amountText,
            onValueChange = { s ->
                if (s.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amountText = s
            },
            label = { Text("金额") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        Text("一级分类", Modifier.padding(top = 16.dp, bottom = 8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tops.forEach { c ->
                FilterChip(
                    selected = selectedTop == c.id,
                    onClick = { selectedTop = c.id },
                    label = { Text(c.name) }
                )
            }
        }

        if (subs.isNotEmpty()) {
            Text("二级分类", Modifier.padding(top = 12.dp, bottom = 8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                subs.forEach { c ->
                    FilterChip(
                        selected = selectedSub == c.id,
                        onClick = { selectedSub = c.id },
                        label = { Text(c.name) }
                    )
                }
            }
        }

        Text("账户", Modifier.padding(top = 16.dp, bottom = 8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            accounts.forEach { a ->
                FilterChip(
                    selected = selectedAccount == a.id,
                    onClick = { selectedAccount = a.id },
                    label = { Text(a.name) }
                )
            }
        }

        OutlinedTextField(
            value = remark,
            onValueChange = { remark = it },
            label = { Text("备注（可选）") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )

        Button(
            onClick = {
                val amount = amountText.toDoubleOrNull() ?: return@Button
                val now = LocalDateTime.now()
                val iso = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
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
            },
            enabled = amountText.toDoubleOrNull() != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            Text("保存")
        }

        Spacer(Modifier.height(32.dp))
    }
}