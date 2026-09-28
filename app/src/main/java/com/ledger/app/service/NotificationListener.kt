package com.ledger.app.service

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ledger.app.LedgerApp
import com.ledger.app.util.PaymentCapture
import com.ledger.app.util.PaymentParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val targetPackages = setOf(
        "com.tencent.mm",              // 微信
        "com.eg.android.AlipayGphone"  // 支付宝
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            val statusBarNotification = sbn ?: return
            if (statusBarNotification.packageName !in targetPackages) return

            val extras = statusBarNotification.notification?.extras ?: return
            val full = extractNotificationText(extras)
            if (full.isBlank() || !PaymentParser.isPaymentSuccess(full)) return

            val app = applicationContext as? LedgerApp ?: return
            val parsed = PaymentParser.parse(full)
            if (parsed.amount == null) return

            scope.launch {
                PaymentCapture.recordIfNew(
                    app = app,
                    source = "notification",
                    rawText = full,
                    parsed = parsed
                )
            }
        } catch (_: Exception) {
            // 通知回调异常兜底，保证监听服务持续运行
        }
    }

    private fun extractNotificationText(extras: Bundle): String {
        val parts = ArrayList<CharSequence>(8)
        extras.getCharSequence(Notification.EXTRA_TITLE)?.let(parts::add)
        extras.getCharSequence(Notification.EXTRA_TEXT)?.let(parts::add)
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let(parts::add)
        extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.let(parts::add)
        extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.let(parts::add)
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach(parts::add)
        return parts
            .map(CharSequence::toString)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(" ")
    }
}
