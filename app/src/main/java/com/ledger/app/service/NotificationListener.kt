package com.ledger.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ledger.app.LedgerApp
import com.ledger.app.data.entity.PendingTransaction
import com.ledger.app.util.Format
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
    private val paymentKeywords = listOf("支付", "付款", "收款", "消费", "支出")

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val statusBarNotification = sbn ?: return
        if (statusBarNotification.packageName !in targetPackages) return

        val extras = statusBarNotification.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val full = "$title $text"
        if (full.isBlank() || paymentKeywords.none { full.contains(it) }) return

        val parsed = PaymentParser.parse(full)
        val amount = parsed.amount ?: return
        val app = applicationContext as? LedgerApp ?: return

        scope.launch {
            val now = Format.nowIso()
            app.db.pendingDao().insert(
                PendingTransaction(
                    source = "notification",
                    rawText = full,
                    parsedAmount = amount,
                    parsedMerchant = parsed.merchant,
                    parsedDate = now,
                    parsedType = "expense",
                    confidence = parsed.confidence,
                    status = "pending",
                    createdAt = now
                )
            )
        }
    }
}