package com.ledger.app

import android.app.Application
import com.ledger.app.data.db.AppDatabase
import com.ledger.app.data.seed.AutoRuleSeeder
import com.ledger.app.data.seed.CategorySeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LedgerApp : Application() {

    val db: AppDatabase by lazy { AppDatabase.get(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            CategorySeeder.seedIfEmpty(db)
            AutoRuleSeeder.seedIfEmpty(db)
        }
    }
}