package com.ledger.app.util

import android.content.Context
import android.net.Uri
import com.ledger.app.data.db.AppDatabase
import com.ledger.app.data.entity.Account
import com.ledger.app.data.entity.Category
import com.ledger.app.data.entity.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object BackupManager {

    /** 导出数据库到指定 Uri */
    suspend fun exportDatabase(context: Context, uri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching<Unit> {
                val dbFile = context.getDatabasePath("ledger.db")
                if (!dbFile.exists()) error("数据库文件不存在")
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    dbFile.inputStream().use { it.copyTo(out) }
                } ?: error("无法打开输出流")
            }
        }

    /** 从 Uri 导入数据库覆盖 */
    suspend fun importDatabase(context: Context, uri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching<Unit> {
                // 先关闭 Room
                AppDatabase.close()

                val dbFile = context.getDatabasePath("ledger.db")
                dbFile.parentFile?.mkdirs()
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dbFile.outputStream().use { input.copyTo(it) }
                } ?: error("无法打开输入流")

                // 删除 WAL/SHM 残留
                File(dbFile.path + "-wal").delete()
                File(dbFile.path + "-shm").delete()
            }
        }

    /** 导出 CSV */
    suspend fun exportCsv(db: AppDatabase, uri: Uri, resolver: android.content.ContentResolver): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val categories = db.categoryDao().observeAllOnce()
                val accounts = db.accountDao().observeAllOnce()
                val txs = db.transactionDao().observeAllOnce()

                val catMap = categories.associateBy { it.id }
                val accMap = accounts.associateBy { it.id }

                resolver.openOutputStream(uri)?.bufferedWriter()?.use { w ->
                    w.write("日期,类型,金额,一级分类,二级分类,账户,备注\n")
                    txs.forEach { t ->
                        val cat = t.categoryId?.let { catMap[it] }
                        val parent = cat?.parentId?.let { catMap[it] }
                        val line = listOf(
                            t.date.replace("T", " "),
                            t.type,
                            "%.2f".format(t.amount),
                            parent?.name ?: "",
                            cat?.name ?: "",
                            t.accountId?.let { accMap[it]?.name } ?: "",
                            (t.remark ?: "").replace(",", "，")
                        ).joinToString(",")
                        w.write(line)
                        w.write("\n")
                    }
                }
                txs.size
            }
        }

    /** 生成默认备份文件名 */
    fun defaultDbFileName(): String {
        val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        return "ledger_$ts.db"
    }

    fun defaultCsvFileName(): String {
        val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        return "ledger_$ts.csv"
    }
}