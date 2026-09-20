package com.ledger.app.util

import android.content.Context
import com.ledger.app.data.db.AppDatabase
import com.ledger.app.data.entity.Category
import com.ledger.app.data.entity.Transaction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class QianjiRecord(
    val key: String,
    val date: String,
    val category: String,
    val type: String,
    val money: Double,
    val currency: String = "CNY",
    val hasbx: Int = 0,
    val username: String? = null,
    val billflag: String? = null,
    val sourceid: String = "",
    val remark: String? = null,
    val asset: Long? = null
)

data class ImportResult(
    val expense: Int,
    val income: Int,
    val refund: Int,
    val reimbursement: Int,
    val skipped: Int
)

object QianjiImporter {

    private val json = Json { ignoreUnknownKeys = true }

    // 钱迹分类 → (一级, 二级, type)
    private val MAP: Map<String, Triple<String, String, String>> = mapOf(
        "三餐" to Triple("餐饮", "三餐", "expense"),
        "零食" to Triple("餐饮", "零食", "expense"),
        "购物" to Triple("购物", "其他", "expense"),
        "交通" to Triple("交通", "公交地铁", "expense"),
        "水费" to Triple("居住", "水费", "expense"),
        "电费" to Triple("居住", "电费", "expense"),
        "网费" to Triple("居住", "网费", "expense"),
        "话费" to Triple("通讯", "话费", "expense"),
        "药品" to Triple("医疗健康", "药品", "expense"),
        "就诊" to Triple("医疗健康", "就诊", "expense"),
        "书籍" to Triple("学习", "书籍", "expense"),
        "考试" to Triple("学习", "考试", "expense"),
        "校园" to Triple("学习", "文具", "expense"),
        "门票" to Triple("娱乐", "门票", "expense"),
        "酒店" to Triple("娱乐", "酒店", "expense"),
        "借钱" to Triple("人情", "借钱", "expense"),
        "其它" to Triple("其他", "其他", "expense"),
        "日常" to Triple("其他", "日常", "expense"),
        "收红包" to Triple("家庭支持", "红包", "income"),
        "生活费" to Triple("家庭支持", "生活费", "income"),
        "工资" to Triple("职业收入", "工资", "income"),
        "本月结余" to Triple("其他收入", "本月结余", "income")
    )

    // 网费备注关键词 → 二级分类
    private val NET_KEYWORDS = mapOf(
        "金铲铲" to ("娱乐" to "游戏"),
        "荒野大镖客" to ("娱乐" to "游戏"),
        "钢铁雄心" to ("娱乐" to "游戏"),
        "游戏" to ("娱乐" to "游戏"),
        "电影" to ("娱乐" to "电影"),
        "彩票" to ("娱乐" to "休闲"),
        "抓娃娃" to ("娱乐" to "休闲"),
        "打牌" to ("娱乐" to "休闲"),
        "微信读书" to ("学习" to "书籍")
    )

    /** 解析 JSON 文件（不写库），返回记录列表 */
    fun parse(jsonText: String): List<QianjiRecord> {
        return json.decodeFromString<List<QianjiRecord>>(jsonText)
    }

    /** 执行导入 */
    suspend fun import(
        db: AppDatabase,
        records: List<QianjiRecord>,
        defaultAccountName: String = "钱迹导入"
    ): ImportResult {
        val accountDao = db.accountDao()
        val existing = accountDao.findByName(defaultAccountName)
        val accountId: Long = existing?.id ?: accountDao.insert(
            com.ledger.app.data.entity.Account(
                name = defaultAccountName,
                type = "other",
                sortOrder = 99
            )
        )

        var expense = 0
        var income = 0
        var refund = 0
        var reimburse = 0
        var skipped = 0

        val txDao = db.transactionDao()

        for (r in records) {
            if (txDao.findBySourceKey(r.key) != null) {
                skipped++
                continue
            }

            if (r.type == "报销记录") {
                skipped++
                continue
            }

            val txType = when (r.type) {
                "支出" -> "expense"
                "收入" -> "income"
                "退款" -> "refund"
                "报销" -> "reimbursement"
                else -> "expense"
            }

            val categoryId = resolveCategory(db, r.category, r.type, r.remark)

            txDao.insert(
                Transaction(
                    sourceKey = r.key,
                    type = txType,
                    amount = r.money,
                    categoryId = categoryId,
                    accountId = accountId,
                    date = normalizeDate(r.date),
                    remark = r.remark,
                    relatedKey = r.sourceid.ifBlank { null },
                    rawCategory = r.category,
                    rawType = r.type,
                    createdAt = LocalDateTime.now().toString(),
                    updatedAt = LocalDateTime.now().toString()
                )
            )

            when (txType) {
                "expense" -> expense++
                "income" -> income++
                "refund" -> refund++
                "reimbursement" -> reimburse++
            }
        }

        return ImportResult(expense, income, refund, reimburse, skipped)
    }

    private suspend fun resolveCategory(
        db: AppDatabase,
        rawCategory: String,
        rawType: String,
        remark: String?
    ): Long? {
        // 网费拆分
        if (rawCategory == "网费" && !remark.isNullOrBlank()) {
            for ((kw, pair) in NET_KEYWORDS) {
                if (remark.contains(kw)) {
                    return getOrCreate(db, pair.first, pair.second,
                        if (rawType == "收入") "income" else "expense")
                }
            }
        }

        val (parent, child, type) = MAP[rawCategory]
            ?: Triple("其他", "其他", if (rawType == "收入") "income" else "expense")

        return getOrCreate(db, parent, child, type)
    }

    private suspend fun getOrCreate(
        db: AppDatabase,
        parent: String,
        child: String,
        type: String
    ): Long {
        val dao = db.categoryDao()
        val parentCat = dao.find(parent, null, type)
            ?: Category(name = parent, parentId = null, type = type, isBuiltin = true).let {
                val newId = dao.insert(it)
                Category(id = newId, name = parent, type = type)
            }
        val parentId = parentCat.id
        val childCat = dao.find(child, parentId, type)
        if (childCat != null) return childCat.id
        val newChildId = dao.insert(
            Category(name = child, parentId = parentId, type = type, isBuiltin = true)
        )
        return newChildId
    }

    /** "2026-09-20 11:47:57" → "2026-09-20T11:47:57" */
    private fun normalizeDate(s: String): String {
        return s.replace(" ", "T")
    }
}