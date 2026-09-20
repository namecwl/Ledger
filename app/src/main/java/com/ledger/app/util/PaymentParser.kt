package com.ledger.app.util

data class ParsedPayment(
    val amount: Double?,
    val merchant: String?,
    val confidence: Double
)

object PaymentParser {

    // 针对微信/支付宝通知的通用规则
    private val RULES = listOf(
        // 微信支付凭证：¥11.00
        Regex("""已支付[¥￥]([\d,]+\.?\d*)"""),
        Regex("""微信支付[¥￥]([\d,]+\.?\d*)"""),
        Regex("""支付金额[：:]\s*[¥￥]?([\d,]+\.?\d*)"""),
        Regex("""付款[¥￥]([\d,]+\.?\d*)"""),
        // 支付宝
        Regex("""支付宝.*?([\d,]+\.\d{2})\s*元"""),
        Regex("""成功付款\s*([\d,]+\.\d{2})"""),
        Regex("""支出\s*[¥￥]([\d,]+\.?\d*)""")
    )

    private val MERCHANT_RULES = listOf(
        Regex("""收款方[：:]\s*(.+)"""),
        Regex("""商户[：:]\s*(.+)"""),
        Regex("""向(.+?)付款"""),
        Regex("""在(.+?)消费"""),
        Regex("""付款给(.+)""")
    )

    fun parse(text: String): ParsedPayment {
        var amount: Double? = null
        var merchant: String? = null

        for (r in RULES) {
            val m = r.find(text) ?: continue
            val raw = m.groupValues[1].replace(",", "")
            amount = raw.toDoubleOrNull()
            if (amount != null) break
        }

        for (r in MERCHANT_RULES) {
            val m = r.find(text) ?: continue
            merchant = m.groupValues[1].trim().take(50)
            if (merchant.isNotBlank()) break
        }

        val confidence = when {
            amount != null && merchant != null -> 1.0
            amount != null -> 0.6
            else -> 0.2
        }

        return ParsedPayment(amount, merchant, confidence)
    }
}