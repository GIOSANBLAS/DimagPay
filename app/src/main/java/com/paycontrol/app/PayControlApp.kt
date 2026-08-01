package com.paycontrol.app

import android.app.Application
import com.paycontrol.app.data.backup.BackupManager
import com.paycontrol.app.data.contacts.ContactsRepository
import com.paycontrol.app.data.local.AppDatabase
import com.paycontrol.app.data.preferences.UserPreferencesRepository
import com.paycontrol.app.data.repository.ClientRepository
import com.paycontrol.app.data.repository.FinanceRepository
import com.paycontrol.app.data.repository.SupplierRepository
import com.paycontrol.app.reminders.DebtReminderNotifications
import com.paycontrol.app.reminders.DebtReminderScheduler
import com.paycontrol.app.ui.widget.BalanceWidgetProvider
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class PayControlApp : Application() {

    @Inject lateinit var database: AppDatabase
    @Inject lateinit var userPreferences: UserPreferencesRepository
    @Inject lateinit var contactsRepository: ContactsRepository
    @Inject lateinit var financeRepository: FinanceRepository
    @Inject lateinit var clientRepository: ClientRepository
    @Inject lateinit var supplierRepository: SupplierRepository
    @Inject lateinit var backupManager: BackupManager

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        DebtReminderNotifications.ensureChannel(this)
        if (::userPreferences.isInitialized && userPreferences.debtRemindersEnabled.value) {
            DebtReminderScheduler.schedule(this)
        }
    }

    fun refreshBalanceWidgets() {
        BalanceWidgetProvider.requestUpdate(this)
    }
}
