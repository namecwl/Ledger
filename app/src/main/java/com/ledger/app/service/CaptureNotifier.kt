package com.ledger.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ledger.app.MainActivity
import com.ledger.app.R
import com.ledger.app.util.Format
import java.util.concurrent.atomic.AtomicInteger

/**
 * 识别到一笔交易后发出的通知：既给用户“确实在工作”的反馈，点击还能直接跳到待确认页。
 */
object CaptureNotifier {

    private const val CHANNEL_ID = "ledger_capture"
    private val idGenerator = AtomicInteger(2000)

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "自动记账识别结果",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "识别到微信/支付宝交易时提醒去确认"
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun notifyCaptured(
        context: Context,
        amount: Double,
        kind: String,
        sourceLabel: String
    ) {
        try {
            ensureChannel(context)
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("go_pending", true)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("已识别一笔$kind：¥${Format.money(amount)}")
                .setContentText("来自${sourceLabel}，点击去确认入账")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(idGenerator.incrementAndGet(), notification)
        } catch (_: Throwable) {
        }
    }
}
