package com.ledger.app.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    BottomItem("record", "记一笔", Icons.Default.Add),
    BottomItem("stats", "统计", Icons.Default.PieChart)
)

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val vm: MainViewModel = viewModel()
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
                    currentRoute = currentRoute,
                    onSelect = { route ->
                        if (route != currentRoute) {
                            nav.navigate(route) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "bills",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable("bills") {
                BillsScreen(
                    vm = vm,
                    onPending = { nav.navigate("pending") },
                    onSettings = { nav.navigate("settings") }
                )
            }
            composable("record") { RecordScreen(vm) }
            composable("pending") {
                PendingScreen(vm = vm, onBack = { nav.popBackStack() })
            }
            composable("stats") { StatsScreen(vm) }
            composable("settings") {
                SettingsScreen(nav = nav, vm = vm, onBack = { nav.popBackStack() })
            }
            composable("category_manage") { CategoryManageScreen(nav, vm) }
            composable("account_manage") { AccountManageScreen(nav, vm) }
            composable("update") { UpdateScreen(nav) }
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
    currentRoute: String?,
    onSelect: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(62.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainBottomItems.forEach { item ->
                val selected = currentRoute == item.route
                val isRecord = item.route == "record"
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable { onSelect(item.route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isRecord) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Text(
                        item.label,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(top = if (isRecord) 3.dp else 5.dp)
                    )
                }
            }
        }
    }
}
