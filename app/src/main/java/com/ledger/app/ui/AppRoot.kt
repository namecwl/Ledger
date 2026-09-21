package com.ledger.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ledger.app.LedgerApp
import com.ledger.app.ui.screens.AccountManageScreen
import com.ledger.app.ui.screens.BillsScreen
import com.ledger.app.ui.screens.CategoryManageScreen
import com.ledger.app.ui.screens.ImportHolder
import com.ledger.app.ui.screens.ImportPreviewScreen
import com.ledger.app.ui.screens.PendingScreen
import com.ledger.app.ui.screens.RecordScreen
import com.ledger.app.ui.screens.SettingsScreen
import com.ledger.app.ui.screens.StatsScreen
import com.ledger.app.ui.screens.UpdateScreen
import com.ledger.app.util.QianjiImporter
import com.ledger.app.vm.MainViewModel
import kotlinx.coroutines.launch

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val MainBottomItems = listOf(
    BottomItem("bills", "账单", Icons.Default.List),
    BottomItem("stats", "统计", Icons.Default.PieChart),
    BottomItem("record", "记一笔", Icons.Default.Add),
    BottomItem("pending", "待确认", Icons.Default.Notifications),
    BottomItem("settings", "设置", Icons.Default.Settings)
)

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val vm: MainViewModel = viewModel()
    val pendingCount by vm.pendingCount.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val entry by nav.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route
    val isMainTab = MainBottomItems.any { it.route == currentRoute }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (isMainTab) {
                LedgerBottomBar(
                    items = MainBottomItems,
                    currentRoute = currentRoute,
                    pendingCount = pendingCount,
                    onSelect = { route ->
                        nav.navigate(route) {
                            popUpTo(nav.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "record",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable("record") { RecordScreen(vm) }
            composable("bills") { BillsScreen(vm) }
            composable("pending") { PendingScreen(vm) }
            composable("stats") { StatsScreen(vm) }
            composable("settings") { SettingsScreen(nav, vm) }
            composable("update") { UpdateScreen(nav) }
            composable("category_manage") { CategoryManageScreen(nav, vm) }
            composable("account_manage") { AccountManageScreen(nav, vm) }
            composable("import_preview") {
                ImportPreviewScreen(
                    nav = nav,
                    records = ImportHolder.records,
                    onConfirm = {
                        scope.launch {
                            val app = context.applicationContext as LedgerApp
                            QianjiImporter.import(app.db, ImportHolder.records)
                            nav.popBackStack("settings", inclusive = false)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LedgerBottomBar(
    items: List<BottomItem>,
    currentRoute: String?,
    pendingCount: Int,
    onSelect: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 14.dp
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                val isRecord = item.route == "record"
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(item.route) },
                    icon = {
                        val icon: @Composable () -> Unit = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = if (isRecord) Modifier.size(23.dp) else Modifier.size(22.dp)
                            )
                        }
                        if (item.route == "pending" && pendingCount > 0) {
                            BadgedBox(badge = { Badge { Text(if (pendingCount > 99) "99+" else "$pendingCount") } }) {
                                icon()
                            }
                        } else if (isRecord) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primaryContainer
                                    ),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            icon()
                        }
                    },
                    label = { Text(item.label) },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}