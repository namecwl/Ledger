package com.ledger.app.service

import android.content.ComponentName
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/** 读取自动记账相关三项系统授权的实时状态，供设置页展示。 */
object ServiceStatus {

    fun accessibilityEnabled(context: Context): Boolean {
        val expected = ComponentName(
            context,
            PaymentAccessibilityService::class.java
        ).flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(":").any { entry ->
            entry.equals(expected, ignoreCase = true) ||
                (
                    entry.contains(context.packageName, ignoreCase = true) &&
                        entry.contains("PaymentAccessibilityService", ignoreCase = true)
                    )
        }
    }

    fun notificationListenerEnabled(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }

    fun batteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
}
