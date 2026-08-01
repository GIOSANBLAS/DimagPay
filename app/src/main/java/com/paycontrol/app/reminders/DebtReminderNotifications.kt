package com.paycontrol.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.paycontrol.app.MainActivity
import com.paycontrol.app.R
import com.paycontrol.app.data.preferences.UserPreferencesRepository
import com.paycontrol.app.domain.model.AppInfo
import com.paycontrol.app.domain.util.AppLog
import com.paycontrol.app.domain.util.Money

object DebtReminderNotifications {

    const val CHANNEL_ID = "dimagpay_reminders"
    private const val NOTIFICATION_ID = 4101
    private const val TAG = "DebtReminderNotif"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_reminders),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_reminders_desc)
        }
        manager.createNotificationChannel(channel)
    }

    fun showReceivablesReminder(
        context: Context,
        totalCents: Long,
        clientsWithDebt: Int
    ) {
        if (totalCents <= 0L || clientsWithDebt <= 0) return
        ensureChannel(context)

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pinEnabled = runCatching {
            UserPreferencesRepository(context).pinEnabled.value
        }.onFailure { error ->
            AppLog.e(TAG, "Error al leer estado de PIN para recordatorio", error)
        }.getOrDefault(false)

        val body = if (pinEnabled) {
            context.getString(R.string.reminder_body_locked)
        } else {
            context.getString(R.string.reminder_body, Money.format(totalCents))
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(AppInfo.APP_NAME)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(
                if (pinEnabled) NotificationCompat.VISIBILITY_SECRET
                else NotificationCompat.VISIBILITY_PRIVATE
            )
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }.onFailure { error ->
            AppLog.e(TAG, "Error al mostrar notificación de cobranza", error)
        }
    }
}
