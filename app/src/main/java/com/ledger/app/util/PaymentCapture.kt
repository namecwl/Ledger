package com.ledger.app.util

import com.ledger.app.LedgerApp
import com.ledger.app.data.entity.PendingTransaction
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime

/**
 * 通知与无障碍服务共用的支付入库入口。
 * 同一笔付款可能同时出现在通知和支付结果页，因此需要跨来源去重。
 */
object PaymentCapture {

    private val insertMutex = Mutex()

    suspend fun recordIfNew(
        app: LedgerApp,
        source: String,
        rawText: String,
        parsed: ParsedPayment = PaymentParser.parse(rawText)
    ): Boolean {
        if (!PaymentParser.isPaymentSuccess(rawText)) return false
        val amount = parsed.amount?.takeIf { it > 0.0 } ?: return false
        val merchant = parsed.merchant?.takeIf { it.isNotBlank() }

        return insertMutex.withLock {
            val now = Format.nowIso()
            val since = Format.iso(LocalDateTime.now().minusSeconds(90))
            val exists = app.db.pendingDao().countRecentMatches(amount, merchant, since) > 0
            if (exists) return@withLock false

            app.db.pendingDao().insert(
                PendingTransaction(
                    source = source,
                    rawText = rawText.take(500),
                    parsedAmount = amount,
                    parsedMerchant = merchant,
                    parsedDate = now,
                    parsedType = "expense",
                    confidence = parsed.confidence,
                    status = "pending",
                    createdAt = now
                )
            )
            true
        }
    }
}

