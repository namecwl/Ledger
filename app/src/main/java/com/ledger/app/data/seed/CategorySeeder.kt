package com.ledger.app.data.seed

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
        Seed("餐饮", listOf("三餐", "零食", "饮料", "水果", "聚餐", "外卖"), "expense", "🍜"),
        Seed("购物", listOf("日用品", "服饰", "数码", "美妆", "家居", "其他"), "expense", "🛍️"),
        Seed("交通", listOf("公交地铁", "打车", "火车", "飞机", "加油停车", "单车"), "expense", "🚗"),
        Seed("居住", listOf("房租", "水费", "电费", "燃气", "网费", "物业", "维修"), "expense", "🏠"),
        Seed("通讯", listOf("话费", "宽带"), "expense", "📱"),
        Seed("医疗健康", listOf("药品", "就诊", "体检", "健身"), "expense", "💊"),
        Seed("学习", listOf("书籍", "考试", "课程", "文具"), "expense", "📚"),
        Seed("娱乐", listOf("游戏", "电影", "门票", "旅行", "酒店", "休闲"), "expense", "🎮"),
        Seed("人情", listOf("红包", "礼物", "请客", "借钱"), "expense", "🎁"),
        Seed("其他", listOf("理发", "日常", "意外损失", "其他"), "expense", "📦")
    )

    private val income = listOf(
        Seed("职业收入", listOf("工资", "奖金", "兼职"), "income", "💰"),
        Seed("家庭支持", listOf("生活费", "红包"), "income", "👨‍👩‍👧"),
        Seed("退款报销", listOf("退款", "报销"), "income", "🔄"),
        Seed("收回借出", listOf("还款"), "income", "💸"),
        Seed("其他收入", listOf("本月结余", "其他"), "income", "📥")
    )

    suspend fun seedIfEmpty(db: AppDatabase) {
        if (db.categoryDao().count() > 0) return

        val dao = db.categoryDao()
        var order = 0
        (expense + income).forEach { s ->
            val pid = dao.insert(
                Category(
                    name = s.parent,
                    parentId = null,
                    type = s.type,
                    icon = s.icon,
                    sortOrder = order++,
                    isBuiltin = true
                )
            )
            var childOrder = 0
            s.children.forEach { child ->
                dao.insert(
                    Category(
                        name = child,
                        parentId = pid,
                        type = s.type,
                        sortOrder = childOrder++,
                        isBuiltin = true
                    )
                )
            }
        }

        if (db.accountDao().count() == 0) {
            val adao = db.accountDao()
            adao.insert(Account(name = "现金", type = "cash", sortOrder = 0, isBuiltin = true))
            adao.insert(Account(name = "支付宝", type = "alipay", sortOrder = 1, isBuiltin = true))
            adao.insert(Account(name = "微信", type = "wechat", sortOrder = 2, isBuiltin = true))
            adao.insert(Account(name = "银行卡", type = "bank", sortOrder = 3, isBuiltin = true))
        }
    }
}