package com.ledger.app.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.app.LedgerApp
import com.ledger.app.data.entity.Account
import com.ledger.app.data.entity.Category
import com.ledger.app.data.entity.Transaction
import com.ledger.app.data.repo.LedgerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = LedgerRepository((app as LedgerApp).db)

    val transactions: Flow<List<Transaction>> = repo.observeTransactions()
    val categories: Flow<List<Category>> = repo.observeCategories()
    val accounts: Flow<List<Account>> = repo.observeAccounts()

    // ===== 账单 =====
    fun saveTransaction(t: Transaction) = viewModelScope.launch {
        if (t.id == 0L) repo.addTransaction(t) else repo.updateTransaction(t)
    }

    fun deleteTransaction(t: Transaction) = viewModelScope.launch {
        repo.deleteTransaction(t)
    }

    // ===== 分类 =====
    fun addCategory(c: Category) = viewModelScope.launch { repo.addCategory(c) }
    fun updateCategory(c: Category) = viewModelScope.launch { repo.updateCategory(c) }
    fun deleteCategory(c: Category) = viewModelScope.launch { repo.deleteCategory(c) }

    // ===== 账户 =====
    fun addAccount(a: Account) = viewModelScope.launch { repo.addAccount(a) }
    fun updateAccount(a: Account) = viewModelScope.launch { repo.updateAccount(a) }
    fun deleteAccount(a: Account) = viewModelScope.launch { repo.deleteAccount(a) }
}