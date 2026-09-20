package com.ledger.app.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.app.LedgerApp
import com.ledger.app.data.entity.Account
import com.ledger.app.data.entity.Category
import com.ledger.app.data.entity.PendingTransaction
import com.ledger.app.data.entity.Transaction
import com.ledger.app.data.repo.LedgerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ledgerApp = app as LedgerApp
    private val repo = LedgerRepository(ledgerApp.db)

    val transactions: Flow<List<Transaction>> = repo.observeTransactions()
    val categories: Flow<List<Category>> = repo.observeCategories()
    val accounts: Flow<List<Account>> = repo.observeAccounts()
    val pendingList: Flow<List<PendingTransaction>> = ledgerApp.db.pendingDao().observePending()
    val pendingCount: Flow<Int> = ledgerApp.db.pendingDao().observePendingCount()

    // 账单
    fun saveTransaction(t: Transaction) = viewModelScope.launch {
        if (t.id == 0L) repo.addTransaction(t) else repo.updateTransaction(t)
    }
    fun deleteTransaction(t: Transaction) = viewModelScope.launch { repo.deleteTransaction(t) }

    // 分类
    fun addCategory(c: Category) = viewModelScope.launch { repo.addCategory(c) }
    fun updateCategory(c: Category) = viewModelScope.launch { repo.updateCategory(c) }
    fun deleteCategory(c: Category) = viewModelScope.launch { repo.deleteCategory(c) }

    // 账户
    fun addAccount(a: Account) = viewModelScope.launch { repo.addAccount(a) }
    fun updateAccount(a: Account) = viewModelScope.launch { repo.updateAccount(a) }
    fun deleteAccount(a: Account) = viewModelScope.launch { repo.deleteAccount(a) }

    // 待确认
    fun confirmPending(p: PendingTransaction, categoryId: Long?, accountId: Long?) =
        viewModelScope.launch {
            val now = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            )
            repo.addTransaction(
                Transaction(
                    type = p.parsedType ?: "expense",
                    amount = p.parsedAmount ?: 0.0,
                    categoryId = categoryId,
                    accountId = accountId,
                    date = p.parsedDate ?: now,
                    remark = p.parsedMerchant,
                    createdAt = now,
                    updatedAt = now
                )
            )
            ledgerApp.db.pendingDao().update(p.copy(status = "confirmed"))
        }

    fun rejectPending(p: PendingTransaction) = viewModelScope.launch {
        ledgerApp.db.pendingDao().update(p.copy(status = "rejected"))
    }

    fun deletePending(p: PendingTransaction) = viewModelScope.launch {
        ledgerApp.db.pendingDao().delete(p)
    }
}