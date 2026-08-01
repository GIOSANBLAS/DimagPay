package com.paycontrol.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.paycontrol.app.PayControlApp
import com.paycontrol.app.ui.screens.accounts.AccountsViewModel
import com.paycontrol.app.ui.screens.backup.BackupViewModel
import com.paycontrol.app.ui.screens.clients.ClientsViewModel
import com.paycontrol.app.ui.screens.dashboard.DashboardViewModel
import com.paycontrol.app.ui.screens.lock.AppLockViewModel
import com.paycontrol.app.ui.screens.lock.PinSettingsViewModel
import com.paycontrol.app.ui.screens.onboarding.OnboardingViewModel
import com.paycontrol.app.ui.screens.reports.ReportsViewModel
import com.paycontrol.app.ui.screens.settings.SettingsViewModel
import com.paycontrol.app.ui.screens.suppliers.SuppliersViewModel
import com.paycontrol.app.ui.screens.transactions.TransactionsViewModel

class AppViewModelFactory(
    private val app: PayControlApp
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(OnboardingViewModel::class.java) ->
                OnboardingViewModel(app.userPreferences, app.financeRepository) as T

            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(
                    app.financeRepository,
                    app.clientRepository,
                    app.userPreferences
                ) as T

            modelClass.isAssignableFrom(TransactionsViewModel::class.java) ->
                TransactionsViewModel(app.financeRepository) as T

            modelClass.isAssignableFrom(SuppliersViewModel::class.java) ->
                SuppliersViewModel(app.supplierRepository, app.financeRepository) as T

            modelClass.isAssignableFrom(ClientsViewModel::class.java) ->
                ClientsViewModel(app.clientRepository, app.financeRepository) as T

            modelClass.isAssignableFrom(AccountsViewModel::class.java) ->
                AccountsViewModel(app.financeRepository) as T

            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(app, app.userPreferences) as T

            modelClass.isAssignableFrom(ReportsViewModel::class.java) ->
                ReportsViewModel(app, app.financeRepository) as T

            modelClass.isAssignableFrom(BackupViewModel::class.java) ->
                BackupViewModel(app, app.backupManager) as T

            modelClass.isAssignableFrom(AppLockViewModel::class.java) ->
                AppLockViewModel(app.userPreferences) as T

            modelClass.isAssignableFrom(PinSettingsViewModel::class.java) ->
                PinSettingsViewModel(app.userPreferences) as T

            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
