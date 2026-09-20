package com.ledger.app.data.seed

import com.ledger.app.data.db.AppDatabase
import com.ledger.app.data.entity.AutoRule
import java.time.LocalDateTime

object AutoRuleSeeder {

    suspend fun seedIfEmpty(db: AppDatabase) {
        val dao = db.autoRuleDao()
        if (dao.count() > 0) return
        val now = LocalDateTime.now().toString()

        val rules = listOf(
            AutoRule(sourcePackage = "com.tencent.mm",
                pattern = """已支付[¥￥]([\d,]+\.?\d*).*?收款方[：:](.+)""",
                amountGroup = 1, merchantGroup = 2, updatedAt = now),
            AutoRule(sourcePackage = "com.tencent.mm",
                pattern = """微信支付[¥￥]([\d,]+\.?\d*)""",
                amountGroup = 1, updatedAt = now),
            AutoRule(sourcePackage = "com.eg.android.AlipayGphone",
                pattern = """支付宝.*?([\d,]+\.\d{2})\s*元""",
                amountGroup = 1, updatedAt = now),
            AutoRule(sourcePackage = "com.eg.android.AlipayGphone",
                pattern = """成功付款\s*([\d,]+\.\d{2})""",
                amountGroup = 1, updatedAt = now)
        )
        rules.forEach { dao.insert(it) }
    }
}