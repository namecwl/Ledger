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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class BillDayUi(
    val date: String,
    val week: String,
    val expense: Double,
    val income: Double,
    val transactions: List<Transaction>
)

data class BillsMonthUi(
    val days: List<BillDayUi> = emptyList(),
    val expense: Double = 0.0,
    val income: Double = 0.0
)

data class StatsCategoryUi(
    val name: String,
    val amount: Double,
    val colorIndex: Int
)

data class StatsMonthUi(
    val expense: Double = 0.0,
    val income: Double = 0.0,
    val categories: List<StatsCategoryUi> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ledgerApp = app as LedgerApp
    private val repo = LedgerRepository(ledgerApp.db)
    private val prefs = app.getSharedPreferences("ledger", Context.MODE_PRIVATE)
    private val currentMonth = YearMonth.now()

    val categories: StateFlow<List<Category>> = repo.observeCategories()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val accounts: StateFlow<List<Account>> = repo.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val pendingCount: StateFlow<Int> = ledgerApp.db.pendingDao().observePendingCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val pendingList: StateFlow<List<PendingTransaction>> = ledgerApp.db.pendingDao().observePending()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedMonth = MutableStateFlow(currentMonth)
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth

    private val currentMonthTransactions: StateFlow<List<Transaction>> =
        repo.observeTransactionsByRange(
            currentMonth.atDay(1).toString(),
            currentMonth.plusMonths(1).atDay(1).toString()
        ).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val monthTransactions: StateFlow<List<Transaction>> = _selectedMonth
        .flatMapLatest { month ->
            if (month == currentMonth) {
                currentMonthTransactions
            } else {
                repo.observeTransactionsByRange(
                    month.atDay(1).toString(),
                    month.plusMonths(1).atDay(1).toString()
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // 分组、排序、求和全部放到 Default 线程，避免进入账单/统计页时阻塞主线程。
    val billsUi: StateFlow<BillsMonthUi> = monthTransactions
        .map(::buildBillsMonthUi)
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, BillsMonthUi())

    val statsUi: StateFlow<StatsMonthUi> = combine(monthTransactions, categories) { transactions, categoryList ->
        buildStatsMonthUi(transactions, categoryList)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, StatsMonthUi())

    fun selectMonth(month: YearMonth) {
        if (_selectedMonth.value != month) _selectedMonth.value = month
    }

    private val _monthlyBudget = MutableStateFlow(prefs.getFloat("monthly_budget", 0f).toDouble())
    val monthlyBudget: StateFlow<Double> = _monthlyBudget

    fun setMonthlyBudget(value: Double) {
        prefs.edit().putFloat("monthly_budget", value.toFloat()).apply()
        _monthlyBudget.value = value
    }

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
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val todayBudget: StateFlow<BudgetState?> = combine(
        currentMonthTransactions,
        _monthlyBudget
    ) { transactions, budget ->
        if (budget <= 0) null else BudgetCalculator.compute(budget, transactions)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun saveTransaction(transaction: Transaction) = viewModelScope.launch {
        if (transaction.id == 0L) repo.addTransaction(transaction)
        else repo.updateTransaction(transaction)
    }

    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch {
        repo.deleteTransaction(transaction)
    }

    fun addCategory(category: Category) = viewModelScope.launch { repo.addCategory(category) }
    fun updateCategory(category: Category) = viewModelScope.launch { repo.updateCategory(category) }
    fun deleteCategory(category: Category) = viewModelScope.launch { repo.deleteCategory(category) }

    fun addAccount(account: Account) = viewModelScope.launch { repo.addAccount(account) }
    fun updateAccount(account: Account) = viewModelScope.launch { repo.updateAccount(account) }
    fun deleteAccount(account: Account) = viewModelScope.launch { repo.deleteAccount(account) }

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

    private companion object {
        val InflowTypes = setOf("income", "refund", "reimbursement")
        val WeekNames = listOf("一", "二", "三", "四", "五", "六", "日")

        fun buildBillsMonthUi(transactions: List<Transaction>): BillsMonthUi {
            if (transactions.isEmpty()) return BillsMonthUi()
            val grouped = HashMap<String, MutableList<Transaction>>(32)
            for (transaction in transactions) {
                val date = transaction.date.take(10)
                grouped.getOrPut(date) { ArrayList(8) }.add(transaction)
            }
            val days = ArrayList<BillDayUi>(grouped.size)
            var monthExpense = 0.0
            var monthIncome = 0.0
            for ((date, dayTransactions) in grouped) {
                var expense = 0.0
                var income = 0.0
                for (transaction in dayTransactions) {
                    if (transaction.type == "expense") expense += transaction.amount
                    if (transaction.type in InflowTypes) income += transaction.amount
                }
                monthExpense += expense
                monthIncome += income
                val week = runCatching {
                    val parsed = LocalDate.parse(date)
                    "周${WeekNames[parsed.dayOfWeek.value - 1]}"
                }.getOrDefault("")
                days += BillDayUi(date, week, expense, income, dayTransactions)
            }
            days.sortByDescending { it.date }
            return BillsMonthUi(days, monthExpense, monthIncome)
        }

        fun buildStatsMonthUi(
            transactions: List<Transaction>,
            categories: List<Category>
        ): StatsMonthUi {
            if (transactions.isEmpty()) return StatsMonthUi()
            val categoryMap = categories.associateBy { it.id }
            var expenseTotal = 0.0
            var incomeTotal = 0.0
            val grouped = HashMap<String, Double>()
            for (transaction in transactions) {
                when (transaction.type) {
                    "expense" -> {
                        expenseTotal += transaction.amount
                        val category = transaction.categoryId?.let(categoryMap::get)
                        val parent = category?.parentId?.let(categoryMap::get)
                        val name = parent?.name ?: category?.name ?: "未分类"
                        grouped[name] = (grouped[name] ?: 0.0) + transaction.amount
                    }
                    "income", "refund", "reimbursement" -> incomeTotal += transaction.amount
                }
            }
            val categoryStats = grouped.entries
                .sortedByDescending { it.value }
                .mapIndexed { index, entry ->
                    StatsCategoryUi(entry.key, entry.value, index)
                }
            return StatsMonthUi(expenseTotal, incomeTotal, categoryStats)
        }
    }
}