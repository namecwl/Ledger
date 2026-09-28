package com.ledger.app.data.seed

import androidx.room.withTransaction
import com.ledger.app.data.db.AppDatabase
import com.ledger.app.data.entity.Account
import com.ledger.app.data.entity.Category

object CategorySeeder {

    data class Seed(
        val parent: String,
        val children: List<String>,
        val type: String,
        val icon: String
    )

    private val expense = listOf(
        Seed("餐饮", listOf("三餐", "外卖", "饮品零食"), "expense", "🍜"),
        Seed("交通", listOf("公交地铁", "打车", "加油停车"), "expense", "🚗"),
        Seed("购物", listOf("日用品", "服饰", "数码"), "expense", "🛍️"),
        Seed("居住", listOf("房租房贷", "水电燃气", "物业维修"), "expense", "🏠"),
        Seed("娱乐", listOf("电影游戏", "旅行", "其他娱乐"), "expense", "🎮"),
        Seed("医疗", listOf("药品", "就诊"), "expense", "💊"),
        Seed("学习", listOf("书籍", "课程"), "expense", "📚"),
        Seed("人情", listOf("红包", "礼物"), "expense", "🎁"),
        Seed("其他", emptyList(), "expense", "📦")
    )

    private val income = listOf(
        Seed("工资", emptyList(), "income", "💰"),
        Seed("兼职奖金", listOf("兼职", "奖金"), "income", "💼"),
        Seed("红包", emptyList(), "income", "🧧"),
        Seed("退款报销", listOf("退款", "报销"), "income", "🔄"),
        Seed("其他收入", emptyList(), "income", "📥")
    )

    suspend fun seedIfEmpty(db: AppDatabase) {
        if (db.categoryDao().count() > 0) return

        db.withTransaction {
            val dao = db.categoryDao()
            var order = 0
            (expense + income).forEach { seed ->
                val parentId = dao.insert(
                    Category(
                        name = seed.parent,
                        parentId = null,
                        type = seed.type,
                        icon = seed.icon,
                        sortOrder = order++,
                        isBuiltin = true
                    )
                )
                var childOrder = 0
                seed.children.forEach { child ->
                    dao.insert(
                        Category(
                            name = child,
                            parentId = parentId,
                            type = seed.type,
                            sortOrder = childOrder++,
                            isBuiltin = true
                        )
                    )
                }
            }

            if (db.accountDao().count() == 0) {
                val accountDao = db.accountDao()
                accountDao.insert(Account(name = "现金", type = "cash", sortOrder = 0, isBuiltin = true))
                accountDao.insert(Account(name = "支付宝", type = "alipay", sortOrder = 1, isBuiltin = true))
                accountDao.insert(Account(name = "微信", type = "wechat", sortOrder = 2, isBuiltin = true))
                accountDao.insert(Account(name = "银行卡", type = "bank", sortOrder = 3, isBuiltin = true))
            }
        }
    }
}
