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
    private val successKeywords = listOf("支付成功", "付款成功", "转账成功", "已支付", "成功收款", "成功付款")
    private var lastFingerprint: String? = null
    private var lastHandleAt = 0L

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
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 整个回调必须兜底：任何异常都不能让服务进程崩溃，否则系统会把无障碍开关弹回关闭
        try {
            val accessibilityEvent = event ?: return
            val packageName = accessibilityEvent.packageName?.toString() ?: return
            if (packageName !in targetPackages) return

            // 节流：界面内容变化事件非常密集，最短间隔处理一次，避免主线程被拖垮触发 ANR
            val now = System.currentTimeMillis()
            if (now - lastHandleAt < MIN_INTERVAL_MS) return

            val root = rootInActiveWindow ?: return
            val text = collectText(root, maxNodes = 260, maxDepth = 18)
            if (text.isBlank() || successKeywords.none { text.contains(it) }) return

            lastHandleAt = now

            val parsed = PaymentParser.parse(text)
            val amount = parsed.amount ?: return

            val fingerprint = "${amount}_${parsed.merchant ?: ""}_${now / 10_000}"
            if (fingerprint == lastFingerprint) return
            lastFingerprint = fingerprint

            val app = applicationContext as? LedgerApp ?: return
            scope.launch {
                try {
                    val nowIso = Format.nowIso()
                    app.db.pendingDao().insert(
                        PendingTransaction(
                            source = "accessibility",
                            rawText = text.take(500),
                            parsedAmount = amount,
                            parsedMerchant = parsed.merchant,
                            parsedDate = nowIso,
                            parsedType = "expense",
                            confidence = parsed.confidence,
                            status = "pending",
                            createdAt = nowIso
                        )
                    )
                } catch (throwable: Throwable) {
                    Log.e(TAG, "写入待确认账单失败", throwable)
                }
            }
        } catch (throwable: Throwable) {
            Log.e(TAG, "onAccessibilityEvent 异常", throwable)
        }
    }

    /** 带节点数与深度上限的文本收集，命中成功关键词后提前结束，避免在主线程长时间遍历导致 ANR */
    private fun collectText(node: AccessibilityNodeInfo?, maxNodes: Int, maxDepth: Int): String {
        val builder = StringBuilder()
        var count = 0

        fun containsKeyword(): Boolean = successKeywords.any { builder.contains(it) }

        fun walk(current: AccessibilityNodeInfo?, depth: Int) {
            if (current == null || count >= maxNodes || depth > maxDepth) return
            if (containsKeyword()) return
            count++
            current.text?.let { builder.append(it).append(' ') }
            current.contentDescription?.let { builder.append(it).append(' ') }
            if (containsKeyword()) return
            for (index in 0 until current.childCount) {
                walk(current.getChild(index), depth + 1)
                if (containsKeyword() || count >= maxNodes) return
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
    }
}
