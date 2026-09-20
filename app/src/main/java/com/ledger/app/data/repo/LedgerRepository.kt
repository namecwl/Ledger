package com.ledger.app.data.repo

import com.ledger.app.data.db.AppDatabase
import com.ledger.app.data.entity.Account
import com.ledger.app.data.entity.Category
import com.ledger.app.data.entity.Transaction
import kotlinx.coroutines.flow.Flow

class LedgerRepository(private val db: AppDatabase) {

    fun observeTransactions(): Flow<List<Transaction>> = db.transactionDao().observeAll()

    fun observeTransactionsByRange(start: String, end: String): Flow<List<Transaction>> =
        db.transactionDao().observeByRange(start, end)

    fun observeCategories(): Flow<List<Category>> = db.categoryDao().observeAll()
    fun observeAccounts(): Flow<List<Account>> = db.accountDao().observeAll()

    suspend fun addTransaction(t: Transaction): Long = db.transactionDao().insert(t)
    suspend fun updateTransaction(t: Transaction) = db.transactionDao().update(t)
    suspend fun deleteTransaction(t: Transaction) = db.transactionDao().delete(t)

    suspend fun addCategory(c: Category): Long = db.categoryDao().insert(c)
    suspend fun updateCategory(c: Category) = db.categoryDao().update(c)
    suspend fun deleteCategory(c: Category) = db.categoryDao().delete(c)

    suspend fun addAccount(a: Account): Long = db.accountDao().insert(a)
    suspend fun updateAccount(a: Account) = db.accountDao().update(a)
    suspend fun deleteAccount(a: Account) = db.accountDao().delete(a)
}