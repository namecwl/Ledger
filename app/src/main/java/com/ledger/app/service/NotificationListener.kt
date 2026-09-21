package com.ledger.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.ledger.app.LedgerApp
import com.ledger.app.data.entity.PendingTransaction
import com.ledger.app.util.Format
import com.ledger.app.util.PaymentParser
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationListener : NotificationListenerService() {

    private val coroutineHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "协程异常", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineHandler)

    private val targetPackages = setOf(
        "com.tencent.mm",              // 微信
        "com.eg.android.AlipayGphone"  // 支付宝
    )

    private var lastAmount = -1.0
    private var lastName: String? = null
    private var lastSavedAt = 0L

    override fun onListenerConnected() {
        super.onListenerConnected()
        CaptureNotifier.ensureChannel(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            val statusBarNotification = sbn ?: return
            val packageName = statusBarNotification.packageName ?: return
            if (packageName !in targetPackages) return

            val full = extractText(statusBarNotification.notification)
            if (!PaymentParser.canHandle(full)) return

            val parsed = PaymentParser.parse(full)
            val amount = parsed.amount ?: return

            val label = if (packageName.contains("tencent")) "微信" else "支付宝"
            val displayName = parsed.merchant ?: "$label${parsed.kind}"

            val now = System.currentTimeMillis()
            // 同一条通知可能因更新多次回调，30 秒内相同金额 + 名称只保留一次
            if (now - lastSavedAt < DEDUP_WINDOW_MS &&
                amount == lastAmount && displayName == lastName
            ) {
                return
            }
            lastSavedAt = now
            lastAmount = amount
            lastName = displayName

            val app = applicationContext as? LedgerApp ?: return
            scope.launch {
                try {
                    val nowIso = Format.nowIso()
                    app.db.pendingDao().insert(
                        PendingTransaction(
                            source = "notification",
                            rawText = full.take(500),
                            parsedAmount = amount,
                            parsedMerchant = displayName,
                            parsedDate = nowIso,
                            parsedType = parsed.type,
                            confidence = (parsed.confidence + 0.1).coerceAtMost(1.0),
                            status = "pending",
                            createdAt = nowIso
                        )
                    )
                    CaptureNotifier.notifyCaptured(
                        context = applicationContext,
                        amount = amount,
                        kind = parsed.kind,
                        sourceLabel = "${label}通知"
                    )
                } catch (throwable: Throwable) {
                    Log.e(TAG, "写入待确认账单失败", throwable)
                }
            }
        } catch (throwable: Throwable) {
            Log.e(TAG, "onNotificationPosted 异常", throwable)
        }
    }

    /** 汇总通知里所有可能含金额的文本字段：标题、正文、大文本、副文本、收件箱多行 */
    private fun extractText(notification: Notification?): String {
        val extras = notification?.extras ?: return ""
        val parts = mutableListOf<String>()

        extras.getCharSequence(Notification.EXTRA_TITLE)?.let { parts.add(it.toString()) }
        extras.getCharSequence(Notification.EXTRA_TEXT)?.let { parts.add(it.toString()) }
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let { parts.add(it.toString()) }
        extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.let { parts.add(it.toString()) }
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach { line ->
            line?.let { parts.add(it.toString()) }
        }
        return parts.filter { it.isNotBlank() }.distinct().joinToString(" ").trim()
    }

    private companion object {
        const val TAG = "PayNotify"
        const val DEDUP_WINDOW_MS = 30_000L
    }
}
