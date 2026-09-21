package com.ledger.app.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

private tailrec fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

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

    // 从“识别到交易”的通知点进来时，直接打开待确认页
    LaunchedEffect(Unit) {
        val activity = context.findActivity()
        val intent = activity?.intent
        if (intent?.getBooleanExtra("go_pending", false) == true) {
            nav.navigate("pending")
            intent.removeExtra("go_pending")
        }
    }

    // Android 13+ 需要通知权限，否则识别结果通知和保活通知都不显示
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (isMainTab) {
                LedgerBottomBar(
                    currentRoute = currentRoute,
                    pendingCount = pendingCount,
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
            startDestination = "record",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
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
    currentRoute: String?,
    pendingCount: Int,
    onSelect: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(66.dp),
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
                    Box(contentAlignment = Alignment.Center) {
                        if (isRecord) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primaryContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
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
                        if (item.route == "pending" && pendingCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(MaterialTheme.colorScheme.error, CircleShape)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    if (pendingCount > 99) "99+" else "$pendingCount",
                                    color = MaterialTheme.colorScheme.onError,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        item.label,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}