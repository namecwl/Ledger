package com.ledger.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ledger.app.LedgerApp
import com.ledger.app.data.entity.PendingTransaction
import com.ledger.app.util.PaymentParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class NotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val TARGET_PACKAGES = setOf(
        "com.tencent.mm",              // 微信
        "com.eg.android.AlipayGphone"  // 支付宝
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val sbn = sbn ?: return
        if (sbn.packageName !in TARGET_PACKAGES) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val full = "$title $text"
        if (full.isBlank()) return

        // 必须包含支付相关关键词
        val keywords = listOf("支付", "付款", "收款", "消费", "支出")
        if (keywords.none { full.contains(it) }) return

        val parsed = PaymentParser.parse(full)
        if (parsed.amount == null) return

        val app = applicationContext as? LedgerApp ?: return
        scope.launch {
            app.db.pendingDao().insert(
                PendingTransaction(
                    source = "notification",
                    rawText = full,
                    parsedAmount = parsed.amount,
                    parsedMerchant = parsed.merchant,
                    parsedDate = LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                    ),
                    parsedType = "expense",
                    confidence = parsed.confidence,
                    status = "pending",
                    createdAt = LocalDateTime.now().toString()
                )
            )
        }
    }
}