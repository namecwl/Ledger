package com.ledger.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ledger.app.LedgerApp
import com.ledger.app.data.entity.PendingTransaction
import com.ledger.app.util.PaymentParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PaymentAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val TARGET_PACKAGES = setOf(
        "com.tencent.mm",
        "com.eg.android.AlipayGphone"
    )

    private val SUCCESS_KEYWORDS = listOf("支付成功", "付款成功", "转账成功", "已支付")

    private var lastFingerprint: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val event = event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in TARGET_PACKAGES) return

        val root = rootInActiveWindow ?: return
        val text = collectText(root)
        if (text.isBlank()) return

        if (SUCCESS_KEYWORDS.none { text.contains(it) }) return

        val parsed = PaymentParser.parse(text)
        if (parsed.amount == null) return

        // 指纹去重（金额+时间窗口）
        val fp = "${parsed.amount}_${parsed.merchant ?: ""}_${System.currentTimeMillis() / 10000}"
        if (fp == lastFingerprint) return
        lastFingerprint = fp

        val app = applicationContext as? LedgerApp ?: return
        scope.launch {
            app.db.pendingDao().insert(
                PendingTransaction(
                    source = "accessibility",
                    rawText = text.take(500),
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

    private fun collectText(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""
        val sb = StringBuilder()
        node.text?.let { sb.append(it).append(" ") }
        node.contentDescription?.let { sb.append(it).append(" ") }
        for (i in 0 until node.childCount) {
            sb.append(collectText(node.getChild(i)))
        }
        return sb.toString().trim()
    }

    override fun onInterrupt() {}
}