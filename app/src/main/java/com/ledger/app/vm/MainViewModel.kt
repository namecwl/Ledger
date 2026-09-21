package com.ledger.app.vm

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.app.LedgerApp
import com.ledger.app.data.entity.Account
import com.ledger.app.data.entity.Category
import com.ledger.app.data.entity.PendingTransaction
import com.ledger.app.data.entity.Transaction
import com.ledger.app.data.repo.LedgerRepository
import com.ledger.app.util.BudgetCalculator
import com.ledger.app.util.BudgetState
import com.ledger.app.util.Format
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ledgerApp = app as LedgerApp
    private val repo = LedgerRepository(ledgerApp.db)
    private val prefs = app.getSharedPreferences("ledger", Context.MODE_PRIVATE)
    private val currentMonth = YearMonth.now()

    val categories: StateFlow<List<Category>> = repo.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val accounts: StateFlow<List<Account>> = repo.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingList: StateFlow<List<PendingTransaction>> = ledgerApp.db.pendingDao().observePending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingCount: StateFlow<Int> = ledgerApp.db.pendingDao().observePendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ===== 按月查询（账单页性能关键） =====
    private val _selectedMonth = MutableStateFlow(currentMonth)
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth

    val monthTransactions: StateFlow<List<Transaction>> = _selectedMonth
        .flatMapLatest { ym ->
            repo.observeTransactionsByRange(
                ym.atDay(1).toString(),
                ym.plusMonths(1).atDay(1).toString()
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectMonth(ym: YearMonth) {
        if (_selectedMonth.value != ym) _selectedMonth.value = ym
    }

    // ===== 预算 =====
    private val _monthlyBudget = MutableStateFlow(prefs.getFloat("monthly_budget", 0f).toDouble())
    val monthlyBudget: StateFlow<Double> = _monthlyBudget

    fun setMonthlyBudget(value: Double) {
        prefs.edit().putFloat("monthly_budget", value.toFloat()).apply()
        _monthlyBudget.value = value
    }

    /** 当前月份的预算状态。 */
    val budgetState: StateFlow<BudgetState?> =
        combine(_selectedMonth, monthTransactions, _monthlyBudget) { month, transactions, budget ->
            if (budget <= 0) {
                null
            } else {
                BudgetCalculator.compute(
                    budget,
                    transactions,
                    if (month == currentMonth) LocalDate.now() else month.atEndOfMonth()
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 今日可花，仅在记一笔页面可见时订阅。 */
    val todayBudget: StateFlow<BudgetState?> = combine(
        repo.observeTransactionsByRange(
            currentMonth.atDay(1).toString(),
            currentMonth.plusMonths(1).atDay(1).toString()
        ),
        _monthlyBudget
    ) { transactions, budget ->
        if (budget <= 0) null else BudgetCalculator.compute(budget, transactions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ===== 账单 =====
    fun saveTransaction(transaction: Transaction) = viewModelScope.launch {
        if (transaction.id == 0L) repo.addTransaction(transaction)
        else repo.updateTransaction(transaction)
    }

    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch {
        repo.deleteTransaction(transaction)
    }

    // ===== 分类 =====
    fun addCategory(category: Category) = viewModelScope.launch { repo.addCategory(category) }
    fun updateCategory(category: Category) = viewModelScope.launch { repo.updateCategory(category) }
    fun deleteCategory(category: Category) = viewModelScope.launch { repo.deleteCategory(category) }

    // ===== 账户 =====
    fun addAccount(account: Account) = viewModelScope.launch { repo.addAccount(account) }
    fun updateAccount(account: Account) = viewModelScope.launch { repo.updateAccount(account) }
    fun deleteAccount(account: Account) = viewModelScope.launch { repo.deleteAccount(account) }

    // ===== 待确认 =====
    fun confirmPending(
        pending: PendingTransaction,
        categoryId: Long?,
        accountId: Long?
    ) = viewModelScope.launch {
        val now = Format.nowIso()
        repo.addTransaction(
            Transaction(
                type = pending.parsedType ?: "expense",
                amount = pending.parsedAmount ?: 0.0,
                categoryId = categoryId,
                accountId = accountId,
                date = pending.parsedDate ?: now,
                remark = pending.parsedMerchant,
                createdAt = now,
                updatedAt = now
            )
        )
        ledgerApp.db.pendingDao().update(pending.copy(status = "confirmed"))
    }

    fun rejectPending(pending: PendingTransaction) = viewModelScope.launch {
        ledgerApp.db.pendingDao().update(pending.copy(status = "rejected"))
    }

    fun deletePending(pending: PendingTransaction) = viewModelScope.launch {
        ledgerApp.db.pendingDao().delete(pending)
    }
}