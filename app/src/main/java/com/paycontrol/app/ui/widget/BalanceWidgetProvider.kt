package com.paycontrol.app.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.paycontrol.app.MainActivity
import com.paycontrol.app.PayControlApp
import com.paycontrol.app.R
import com.paycontrol.app.domain.util.AppLog
import com.paycontrol.app.domain.util.Money
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Widget de saldo DimagPay. Si el PIN está activo, no muestra el monto.
 */
class BalanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        refreshWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        requestUpdate(context)
    }

    private fun refreshWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val app = context.applicationContext as? PayControlApp
                val pinOn = app?.userPreferences?.pinEnabled?.value == true
                val balanceText = when {
                    app == null -> context.getString(R.string.widget_loading)
                    pinOn -> context.getString(R.string.widget_locked)
                    else -> runCatching {
                        Money.format(app.financeRepository.getAccountsBalance())
                    }.getOrElse { error ->
                        AppLog.e(TAG, "Error al cargar saldo del widget", error)
                        context.getString(R.string.widget_loading)
                    }
                }
                val clickIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val clickPending = PendingIntent.getActivity(
                    context,
                    0,
                    clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_balance).apply {
                        setTextViewText(R.id.widget_title, context.getString(R.string.app_name))
                        setTextViewText(R.id.widget_balance, balanceText)
                        setOnClickPendingIntent(R.id.widget_root, clickPending)
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } finally {
                scope.cancel()
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BalanceWidget"

        fun requestUpdate(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val ids = manager.getAppWidgetIds(
                ComponentName(appContext, BalanceWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return
            val intent = Intent(appContext, BalanceWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                setPackage(appContext.packageName)
            }
            appContext.sendBroadcast(intent)
        }
    }
}
