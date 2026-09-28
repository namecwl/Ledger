package com.ledger.app.util

import android.content.Context

import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import com.ledger.app.service.PaymentAccessibilityService

object AutomationStatus {

    fun isAccessibilityEnabled(context: Context): Boolean {
        val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
        return manager
            .getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { info ->
                val serviceInfo = info.resolveInfo.serviceInfo
                serviceInfo.packageName == context.packageName &&
                    serviceInfo.name == PaymentAccessibilityService::class.java.name
            }
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        return context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
    }
}

