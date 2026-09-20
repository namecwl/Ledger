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

    fun saveTransaction(t: Transaction) = viewModelScope.launch {
        if (t.id == 0L) repo.addTransaction(t) else repo.updateTransaction(t)
    }

    fun deleteTransaction(t: Transaction) = viewModelScope.launch {
        repo.deleteTransaction(t)
    }
}