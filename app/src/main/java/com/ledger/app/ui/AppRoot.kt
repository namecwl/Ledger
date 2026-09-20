package com.ledger.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ledger.app.ui.screens.AccountManageScreen
import com.ledger.app.ui.screens.BillsScreen
import com.ledger.app.ui.screens.CategoryManageScreen
import com.ledger.app.ui.screens.ImportHolder
import com.ledger.app.ui.screens.ImportPreviewScreen
import com.ledger.app.ui.screens.PendingScreen
import com.ledger.app.ui.screens.RecordScreen
import com.ledger.app.ui.screens.SettingsScreen
import com.ledger.app.ui.screens.StatsScreen
import com.ledger.app.util.QianjiImporter
import com.ledger.app.vm.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val vm: MainViewModel = viewModel()
    val pendingCount by vm.pendingCount.collectAsStateWithLifecycle(initialValue = 0)

    val items = listOf(
        BottomItem("record", "记一笔", Icons.Default.Add),
        BottomItem("bills", "账单", Icons.Default.List),
        BottomItem("pending", "待确认", Icons.Default.Notifications),
        BottomItem("stats", "统计", Icons.Default.PieChart),
        BottomItem("settings", "设置", Icons.Default.Settings)
    )

    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    val isMainTab = items.any { it.route == current }

    Scaffold(
        bottomBar = {
            if (isMainTab) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = current == item.route,
                            onClick = {
                                nav.navigate(item.route) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (item.route == "pending" && pendingCount > 0) {
                                    BadgedBox(badge = { Badge { Text("$pendingCount") } }) {
                                        Icon(item.icon, item.label)
                                    }
                                } else {
                                    Icon(item.icon, item.label)
                                }
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "record",
            modifier = Modifier.padding(padding)
        ) {
            composable("record") { RecordScreen(vm) }
            composable("bills") { BillsScreen(vm) }
            composable("pending") { PendingScreen(vm) }
            composable("stats") { StatsScreen(vm) }
            composable("settings") { SettingsScreen(nav, vm) }
            composable("category_manage") { CategoryManageScreen(nav, vm) }
            composable("account_manage") { AccountManageScreen(nav, vm) }
            composable("import_preview") {
                val scope = CoroutineScope(Dispatchers.Main)
                ImportPreviewScreen(
                    nav = nav,
                    records = ImportHolder.records,
                    onConfirm = {
                        scope.launch {
                            val app = nav.context.applicationContext as com.ledger.app.LedgerApp
                            QianjiImporter.import(app.db, ImportHolder.records)
                            nav.popBackStack("settings", inclusive = false)
                        }
                    }
                )
            }
        }
    }
}