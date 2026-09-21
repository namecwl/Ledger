package com.ledger.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ledger.app.LedgerApp
import com.ledger.app.data.entity.PendingTransaction
import com.ledger.app.util.Format
import com.ledger.app.util.PaymentParser
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PaymentAccessibilityService : AccessibilityService() {

    private val coroutineHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "协程异常", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineHandler)
    private val targetPackages = setOf(
        "com.tencent.mm",
        "com.eg.android.AlipayGphone"
    )
    private var lastAmount = -1.0
    private var lastName: String? = null
    private var lastSavedAt = 0L
    private var lastEventAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        // 代码里再配置一次，和 res/xml 双保险，确保只监听微信/支付宝且能读取窗口内容
        runCatching {
            val info = AccessibilityServiceInfo().apply {
                eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                flags = AccessibilityServiceInfo.DEFAULT
                notificationTimeout = 300L
                packageNames = arrayOf("com.tencent.mm", "com.eg.android.AlipayGphone")
            }
            serviceInfo = info
        }
        KeepAliveService.start(applicationContext)
        CaptureNotifier.ensureChannel(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 整个回调必须兜底：任何异常都不能让服务进程崩溃，否则系统会把无障碍开关弹回关闭
        try {
            val accessibilityEvent = event ?: return
            val packageName = accessibilityEvent.packageName?.toString() ?: return
            if (packageName !in targetPackages) return

            // 节流：界面内容变化事件非常密集，最短间隔处理一次，避免主线程被拖垮触发 ANR
            val now = System.currentTimeMillis()
            if (now - lastEventAt < MIN_INTERVAL_MS) return
            lastEventAt = now

            val root = rootInActiveWindow ?: return
            val text = collectText(root, maxNodes = 320, maxDepth = 20)
            // 待确认制：只要出现交易动作词且能解析出金额就先保留，由用户在待确认页确认/忽略，宁多勿漏
            if (!PaymentParser.canHandle(text)) return

            val parsed = PaymentParser.parse(text)
            val amount = parsed.amount ?: return

            val label = if (packageName.contains("tencent")) "微信" else "支付宝"
            val displayName = parsed.merchant ?: "$label${parsed.kind}"

            // 去重：同一金额 + 同一名称在 30 秒内只保留一次
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
                            source = "accessibility",
                            rawText = text.take(500),
                            parsedAmount = amount,
                            parsedMerchant = displayName,
                            parsedDate = nowIso,
                            parsedType = parsed.type,
                            confidence = parsed.confidence,
                            status = "pending",
                            createdAt = nowIso
                        )
                    )
                    CaptureNotifier.notifyCaptured(
                        context = applicationContext,
                        amount = amount,
                        kind = parsed.kind,
                        sourceLabel = "$label页面"
                    )
                } catch (throwable: Throwable) {
                    Log.e(TAG, "写入待确认账单失败", throwable)
                }
            }
        } catch (throwable: Throwable) {
            Log.e(TAG, "onAccessibilityEvent 异常", throwable)
        }
    }

    /** 带节点数与深度上限的文本收集，避免在主线程长时间遍历导致 ANR */
    private fun collectText(node: AccessibilityNodeInfo?, maxNodes: Int, maxDepth: Int): String {
        val builder = StringBuilder()
        var count = 0

        fun walk(current: AccessibilityNodeInfo?, depth: Int) {
            if (current == null || count >= maxNodes || depth > maxDepth) return
            count++
            current.text?.let { builder.append(it).append(' ') }
            current.contentDescription?.let { builder.append(it).append(' ') }
            for (index in 0 until current.childCount) {
                walk(current.getChild(index), depth + 1)
                if (count >= maxNodes) return
            }
        }

        walk(node, 0)
        return builder.toString().trim()
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        KeepAliveService.stop(applicationContext)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        KeepAliveService.stop(applicationContext)
        super.onDestroy()
    }

    private companion object {
        const val TAG = "PayA11y"
        const val MIN_INTERVAL_MS = 700L
        const val DEDUP_WINDOW_MS = 30_000L
    }
}
