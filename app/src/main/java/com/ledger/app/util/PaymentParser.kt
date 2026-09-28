package com.ledger.app.util

data class ParsedPayment(
    val amount: Double?,
    val merchant: String?,
    val confidence: Double
)

object PaymentParser {

    private val successKeywords = listOf(
        "支付成功", "付款成功", "已支付", "成功付款", "消费成功", "扣款成功", "支出", "消费", "扣款"
    )
    private val ignoredKeywords = listOf(
        "退款", "已收款", "收款到账", "收入", "转入", "退回", "支付失败", "付款失败", "交易失败", "取消支付"
    )

    // 规则按可靠度排序，优先读取“支付/付款成功”附近的金额，避免误把余额当成交易额。
    private val amountRules = listOf(
        Regex("""(?:支付|付款)成功\s*[：:]?\s*[¥￥]?\s*([\d,]+(?:\.\d{1,2})?)"""),
        Regex("""成功(?:支付|付款)\s*[：:]?\s*[¥￥]?\s*([\d,]+(?:\.\d{1,2})?)"""),
        Regex("""已支付\s*[：:]?\s*[¥￥]?\s*([\d,]+(?:\.\d{1,2})?)"""),
        Regex("""微信支付\s*[：:]?\s*[¥￥]\s*([\d,]+(?:\.\d{1,2})?)"""),
        Regex("""(?:支出|消费|扣款)\s*[：:]?\s*[¥￥]?\s*([\d,]+(?:\.\d{1,2})?)\s*元?""")
    )

    private val merchantRules = listOf(
        Regex("""收款方\s*[：:]?\s*([^\s,，。]+)"""),
        Regex("""商户\s*[：:]?\s*([^\s,，。]+)"""),
        Regex("""付款给\s*([^\s,，。]+)"""),
        Regex("""转账给\s*([^\s,，。]+)"""),
        Regex("""向\s*(.{2,30}?)(?:付款|转账)"""),
        Regex("""在\s*(.{2,30}?)(?:消费|支付)""")
    )

    fun isPaymentSuccess(text: String): Boolean {
        val normalized = normalize(text)
        return successKeywords.any(normalized::contains) && ignoredKeywords.none(normalized::contains)
    }

    fun parse(text: String): ParsedPayment {
        val normalized = normalize(text)
        var amount: Double? = null
        var merchant: String? = null

        for (rule in amountRules) {
            val match = rule.find(normalized) ?: continue
            val parsed = match.groupValues.getOrNull(1)
                ?.replace(",", "")
                ?.toDoubleOrNull()
            if (parsed != null && parsed > 0.0) {
                amount = parsed
                break
            }
        }

        for (rule in merchantRules) {
            val match = rule.find(normalized) ?: continue
            val parsed = match.groupValues.getOrNull(1)
                ?.trim()
                ?.trim('：', ':', '，', ',', '。')
                ?.take(50)
            if (!parsed.isNullOrBlank()) {
                merchant = parsed
                break
            }
        }

        val confidence = when {
            amount != null && merchant != null -> 0.95
            amount != null -> 0.75
            else -> 0.2
        }
        return ParsedPayment(amount, merchant, confidence)
    }

    private fun normalize(value: String): String {
        val normalized = buildString(value.length) {
            value.forEach { char ->
                when (char) {
                    in '０'..'９' -> append('0' + (char - '０'))
                    '．', '。' -> append('.')
                    '，' -> append(',')
                    '：' -> append(':')
                    '\u00A0', '\n', '\r', '\t' -> append(' ')
                    else -> append(char)
                }
            }
        }
        return normalized.replace(Regex("""\s+"""), " ").trim()
    }
}
