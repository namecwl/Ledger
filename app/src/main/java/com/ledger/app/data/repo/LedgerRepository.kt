package com.ledger.app.data.repo

import com.ledger.app.data.db.AppDatabase
import com.ledger.app.data.entity.Account
import com.ledger.app.data.entity.Category
import com.ledger.app.data.entity.Transaction
import kotlinx.coroutines.flow.Flow

class LedgerRepository(private val db: AppDatabase) {

    fun observeTransactions(): Flow<List<Transaction>> = db.transactionDao().observeAll()

    fun observeCategories(): Flow<List<Category>> = db.categoryDao().observeAll()

    fun observeAccounts(): Flow<List<Account>> = db.accountDao().observeAll()

    suspend fun addTransaction(t: Transaction): Long = db.transactionDao().insert(t)

    suspend fun updateTransaction(t: Transaction) = db.transactionDao().update(t)

    suspend fun deleteTransaction(t: Transaction) = db.transactionDao().delete(t)
}