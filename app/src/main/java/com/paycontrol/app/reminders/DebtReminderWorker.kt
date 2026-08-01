package com.paycontrol.app.reminders

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.paycontrol.app.data.local.AppDatabase
import com.paycontrol.app.data.preferences.UserPreferencesRepository
import com.paycontrol.app.domain.util.AppLog

/**
 * Recordatorio diario de cuentas por cobrar.
 * Solo notifica cuando hay clientes con saldo pendiente y la preferencia está activa.
 */
class DebtReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            val preferences = UserPreferencesRepository(applicationContext)
            if (!preferences.debtRemindersEnabled.value) {
                return@runCatching Result.success()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    return@runCatching Result.success()
                }
            }

            val dao = AppDatabase.getInstance(applicationContext).clientDao()
            val totalCents = dao.getTotalReceivables()
            val clientsWithDebt = dao.countWithDebt()
            if (totalCents > 0L && clientsWithDebt > 0) {
                DebtReminderNotifications.showReceivablesReminder(
                    context = applicationContext,
                    totalCents = totalCents,
                    clientsWithDebt = clientsWithDebt
                )
            }
            Result.success()
        }.getOrElse { error ->
            AppLog.e(TAG, "Error en recordatorio de cobranza", error)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "DebtReminderWorker"
    }
}
