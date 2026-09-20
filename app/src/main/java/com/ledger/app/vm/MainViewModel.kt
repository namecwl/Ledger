package com.ledger.app.vm

import android.content.Context
import android.app.Application
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ledgerApp = app as LedgerApp
    private val repo = LedgerRepository(ledgerApp.db)
    private val prefs = app.getSharedPreferences("ledger", Context.MODE_PRIVATE)

    // 分类/账户（小数据量，全量 flow）
    val categories: Flow<List<Category>> = repo.observeCategories()
    val accounts: Flow<List<Account>> = repo.observeAccounts()

    // 待确认
    val pendingList: Flow<List<PendingTransaction>> = ledgerApp.db.pendingDao().observePending()
    val pendingCount: Flow<Int> = ledgerApp.db.pendingDao().observePendingCount()

    // ===== 按月查询（账单页性能关键） =====
    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth

    val monthTransactions: StateFlow<List<Transaction>> = _selectedMonth
        .flatMapLatest { ym ->
            val start = ym.atDay(1).toString()
            val end = ym.plusMonths(1).atDay(1).toString()
            repo.observeTransactionsByRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectMonth(ym: YearMonth) { _selectedMonth.value = ym }

    // ===== 预算 =====
    private val _monthlyBudget = MutableStateFlow(prefs.getFloat("monthly_budget", 0f).toDouble())
    val monthlyBudget: StateFlow<Double> = _monthlyBudget

    fun setMonthlyBudget(v: Double) {
        prefs.edit().putFloat("monthly_budget", v.toFloat()).apply()
        _monthlyBudget.value = v
    }

    /** 当前月份的预算状态 */
    val budgetState: StateFlow<BudgetState?> =
        combine(_selectedMonth, monthTransactions, _monthlyBudget) { ym, txs, budget ->
            if (budget <= 0) null
            else BudgetCalculator.compute(budget, txs, if (ym == YearMonth.now()) LocalDate.now() else ym.atEndOfMonth())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** 今日可花（用于记一笔页顶部） */
    private val _todayBudget = MutableStateFlow<BudgetState?>(null)
    val todayBudget: StateFlow<BudgetState?> = _todayBudget

    init {
        // 拉取本月数据计算今日预算
        viewModelScope.launch {
            combine(
                repo.observeTransactionsByRange(
                    YearMonth.now().atDay(1).toString(),
                    YearMonth.now().plusMonths(1).atDay(1).toString()
                ),
                _monthlyBudget
            ) { txs, budget ->
                if (budget <= 0) null
                else BudgetCalculator.compute(budget, txs)
            }.collect { _todayBudget.value = it }
        }
    }

    // ===== 账单 =====
    fun saveTransaction(t: Transaction) = viewModelScope.launch {
        if (t.id == 0L) repo.addTransaction(t) else repo.updateTransaction(t)
    }
    fun deleteTransaction(t: Transaction) = viewModelScope.launch { repo.deleteTransaction(t) }

    // ===== 分类 =====
    fun addCategory(c: Category) = viewModelScope.launch { repo.addCategory(c) }
    fun updateCategory(c: Category) = viewModelScope.launch { repo.updateCategory(c) }
    fun deleteCategory(c: Category) = viewModelScope.launch { repo.deleteCategory(c) }

    // ===== 账户 =====
    fun addAccount(a: Account) = viewModelScope.launch { repo.addAccount(a) }
    fun updateAccount(a: Account) = viewModelScope.launch { repo.updateAccount(a) }
    fun deleteAccount(a: Account) = viewModelScope.launch { repo.deleteAccount(a) }

    // ===== 待确认 =====
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