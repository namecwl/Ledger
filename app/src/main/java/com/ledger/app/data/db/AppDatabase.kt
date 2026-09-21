package com.ledger.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ledger.app.data.dao.AccountDao
import com.ledger.app.data.dao.AutoRuleDao
import com.ledger.app.data.dao.BudgetDao
import com.ledger.app.data.dao.CategoryDao
import com.ledger.app.data.dao.MerchantCategoryMapDao
import com.ledger.app.data.dao.PendingTransactionDao
import com.ledger.app.data.dao.SettingDao
import com.ledger.app.data.dao.TransactionDao
import com.ledger.app.data.entity.Account
import com.ledger.app.data.entity.AutoRule
import com.ledger.app.data.entity.Budget
import com.ledger.app.data.entity.Category
import com.ledger.app.data.entity.MerchantCategoryMap
import com.ledger.app.data.entity.PendingTransaction
import com.ledger.app.data.entity.Setting
import com.ledger.app.data.entity.Transaction

@Database(
    entities = [
        Category::class,
        Account::class,
        Transaction::class,
        PendingTransaction::class,
        AutoRule::class,
        MerchantCategoryMap::class,
        Budget::class,
        Setting::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun pendingDao(): PendingTransactionDao
    abstract fun autoRuleDao(): AutoRuleDao
    abstract fun merchantMapDao(): MerchantCategoryMapDao
    abstract fun budgetDao(): BudgetDao
    abstract fun settingDao(): SettingDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val Migration1To2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `budgets` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`month` TEXT NOT NULL, " +
                        "`amount` REAL NOT NULL, " +
                        "`createdAt` TEXT NOT NULL, " +
                        "`updatedAt` TEXT NOT NULL)"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `settings` (" +
                        "`key` TEXT NOT NULL, " +
                        "`value` TEXT NOT NULL, " +
                        "PRIMARY KEY(`key`))"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ledger.db"
                )
                    .addMigrations(Migration1To2)
                    .build()
                    .also { INSTANCE = it }
            }

        fun close() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}