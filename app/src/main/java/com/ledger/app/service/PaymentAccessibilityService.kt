package com.ledger.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ledger.app.LedgerApp
import com.ledger.app.util.PaymentCapture
import com.ledger.app.util.PaymentParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PaymentAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val targetPackages = setOf(
        "com.tencent.mm",
        "com.eg.android.AlipayGphone"
    )

    // 事件节流：支付结果页内容变化事件非常密集，限制处理频率避免系统判定无响应而自动关闭服务
    private var lastProcessAt = 0L
    private var lastText = ""

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            val accessibilityEvent = event ?: return
            val now = System.currentTimeMillis()
            if (now - lastProcessAt < 700L) return
            lastProcessAt = now

            val packageName = accessibilityEvent.packageName?.toString() ?: return
            if (packageName !in targetPackages) return

            val root = rootInActiveWindow ?: return
            val text = collectText(root, 0)
            if (text.isBlank() || text == lastText) return
            lastText = text
            if (!PaymentParser.isPaymentSuccess(text)) return

            val parsed = PaymentParser.parse(text)
            if (parsed.amount == null) return

            val app = applicationContext as? LedgerApp ?: return
            scope.launch {
                PaymentCapture.recordIfNew(
                    app = app,
                    source = "accessibility",
                    rawText = text,
                    parsed = parsed
                )
            }
        } catch (_: Exception) {
            // 无障碍回调必须全程兜底，任何异常都不能导致服务崩溃（崩溃会被系统自动关闭）
        }
    }

    private fun collectText(node: AccessibilityNodeInfo?, depth: Int): String {
        if (node == null || depth > 20) return ""
        val text = StringBuilder()
        try {
            node.text?.let { text.append(it).append(' ') }
            node.contentDescription?.let { text.append(it).append(' ') }
            val childCount = node.childCount.coerceAtMost(80)
            for (index in 0 until childCount) {
                text.append(collectText(node.getChild(index), depth + 1))
                if (text.length >= 2000) break
            }
        } catch (_: Exception) {
            // 单节点读取失败不影响整体
        }
        return text.toString().trim()
    }

    override fun onInterrupt() = Unit
}
