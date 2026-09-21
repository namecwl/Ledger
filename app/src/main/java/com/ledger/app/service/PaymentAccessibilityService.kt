package com.ledger.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ledger.app.LedgerApp
import com.ledger.app.data.entity.PendingTransaction
import com.ledger.app.util.Format
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
    private val successKeywords = listOf("支付成功", "付款成功", "转账成功", "已支付")
    private var lastFingerprint: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val accessibilityEvent = event ?: return
        val packageName = accessibilityEvent.packageName?.toString() ?: return
        if (packageName !in targetPackages) return

        val root = rootInActiveWindow ?: return
        val text = collectText(root)
        if (text.isBlank() || successKeywords.none { text.contains(it) }) return

        val parsed = PaymentParser.parse(text)
        val amount = parsed.amount ?: return

        val fingerprint = "${amount}_${parsed.merchant ?: ""}_${System.currentTimeMillis() / 10_000}"
        if (fingerprint == lastFingerprint) return
        lastFingerprint = fingerprint

        val app = applicationContext as? LedgerApp ?: return
        scope.launch {
            val now = Format.nowIso()
            app.db.pendingDao().insert(
                PendingTransaction(
                    source = "accessibility",
                    rawText = text.take(500),
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

    private fun collectText(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""
        val text = StringBuilder()
        node.text?.let { text.append(it).append(' ') }
        node.contentDescription?.let { text.append(it).append(' ') }
        for (index in 0 until node.childCount) {
            text.append(collectText(node.getChild(index)))
        }
        return text.toString().trim()
    }

    override fun onInterrupt() = Unit
}