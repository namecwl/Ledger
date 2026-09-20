package com.ledger.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ledger.app.data.dao.AccountDao
import com.ledger.app.data.dao.AutoRuleDao
import com.ledger.app.data.dao.CategoryDao
import com.ledger.app.data.dao.MerchantCategoryMapDao
import com.ledger.app.data.dao.PendingTransactionDao
import com.ledger.app.data.dao.TransactionDao
import com.ledger.app.data.entity.Account
import com.ledger.app.data.entity.AutoRule
import com.ledger.app.data.entity.Category
import com.ledger.app.data.entity.MerchantCategoryMap
import com.ledger.app.data.entity.PendingTransaction
import com.ledger.app.data.entity.Transaction

@Database(
    entities = [
        Category::class,
        Account::class,
        Transaction::class,
        PendingTransaction::class,
        AutoRule::class,
        MerchantCategoryMap::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun pendingDao(): PendingTransactionDao
    abstract fun autoRuleDao(): AutoRuleDao
    abstract fun merchantMapDao(): MerchantCategoryMapDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ledger.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}