package com.ledger.app.util

data class ParsedPayment(
    val amount: Double?,
    val merchant: String?,
    val confidence: Double,
    val type: String = "expense", // expense 支出 / income 收入
    val kind: String = "支付"
)

object PaymentParser {

    // 交易动作词：文本里必须出现这些词之一，才认为是一笔交易，避免把余额/账单数字误识别
    val ACTION_WORDS = listOf(
        "支付", "付款", "转账", "收款", "到账", "收钱", "消费", "支出", "红包",
        "充值", "缴费", "扣费", "退款", "汇款", "提现", "扫码", "金额", "入账", "存入"
    )

    // 命中后判定为“收到钱”的强信号
    private val INCOME_WORDS = listOf(
        "到账", "二维码收款", "微信支付收款", "已收款", "收款成功", "收钱", "收到",
        "退款", "入账", "存入零钱", "已存入", "收款码", "收款¥", "收款￥", "对方已转账"
    )

    // 金额提取：优先匹配“动作词 + 金额”，其次 ¥金额，最后 “金额元”
    private val AMOUNT_PATTERNS = listOf(
        Regex("""(?:金额|支付|付款|消费|支出|充值|缴费|扣费|退款|汇款|提现|转账|收款|到账|收钱|红包|存入)\s*[¥￥]?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)"""),
        Regex("""[¥￥]\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)"""),
        Regex("""([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*元""")
    )

    private val MERCHANT_PATTERNS = listOf(
        Regex("""转账给\s*([^¥￥\s,，。、\d][^¥￥,，。]{0,19})"""),
        Regex("""向\s*([^¥￥\s,，。、\d][^¥￥,，。]{0,19}?)\s*转账"""),
        Regex("""给\s*([^¥￥\s,，。、\d][^¥￥,，。]{0,19}?)\s*(?:转账|发红包|付款|支付)"""),
        Regex("""付款给\s*([^\s,，。]{1,20})"""),
        Regex("""收款方[：:]\s*([^\s,，。¥￥]{1,20})"""),
        Regex("""(?:商户|商家|店铺|门店)[：:]\s*([^\s,，。¥￥]{1,20})"""),
        Regex("""在\s*([^\s,，。]{2,20}?)\s*消费""")
    )

    fun canHandle(text: String): Boolean {
        if (text.isBlank()) return false
        if (ACTION_WORDS.none { text.contains(it) }) return false
        return extractAmount(text) != null
    }

    fun parse(text: String): ParsedPayment {
        val amount = extractAmount(text)
        val merchant = MERCHANT_PATTERNS
            .firstNotNullOfOrNull { regex ->
                regex.find(text)?.groupValues?.getOrNull(1)?.trim()
                    ?.trim('，', ',', '。', ':', '：', ' ')
                    ?.takeIf { it.isNotBlank() }
            }

        val type = detectType(text)
        val kind = kindOf(text, type)

        val confidence = when {
            amount != null && merchant != null -> 0.95
            amount != null && hasStrongSignal(text) -> 0.8
            amount != null -> 0.6
            else -> 0.2
        }

        return ParsedPayment(
            amount = amount,
            merchant = merchant,
            confidence = confidence,
            type = type,
            kind = kind
        )
    }

    private fun extractAmount(text: String): Double? {
        for (regex in AMOUNT_PATTERNS) {
            val match = regex.find(text) ?: continue
            val raw = match.groupValues[1].replace(",", "").replace("，", "")
            val value = raw.toDoubleOrNull() ?: continue
            if (value > 0.0 && value < 100_000_000.0) return value
        }
        return null
    }

    private fun detectType(text: String): String {
        // “收款方/收款商户/收款人”指对方收钱，属于支出，先剔除再判断收入信号
        val masked = text
            .replace("收款方", " ")
            .replace("收款商户", " ")
            .replace("收款人", " ")
        if (INCOME_WORDS.any { masked.contains(it) }) return "income"
        if (Regex("""收款\s*[¥￥]?\s*[0-9]""").containsMatchIn(masked)) return "income"
        if (Regex("""(?:收到|领取了?).{0,6}红包""").containsMatchIn(masked)) return "income"
        return "expense"
    }

    private fun hasStrongSignal(text: String): Boolean =
        INCOME_WORDS.any { text.contains(it) } ||
            listOf("支付成功", "付款成功", "转账成功", "已支付", "成功付款", "转账¥", "转账￥").any { text.contains(it) }

    private fun kindOf(text: String, type: String): String = when {
        text.contains("红包") -> "红包"
        text.contains("转账") -> if (type == "income") "收到转账" else "转账"
        text.contains("退款") -> "退款"
        text.contains("到账") || text.contains("收款") || text.contains("收钱") || text.contains("存入") -> "收款"
        text.contains("消费") -> "消费"
        text.contains("充值") -> "充值"
        text.contains("缴费") -> "缴费"
        text.contains("提现") -> "提现"
        else -> "支付"
    }
}
