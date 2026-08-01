package com.paycontrol.app

import android.app.Application
import com.paycontrol.app.data.contacts.ContactsRepository
import com.paycontrol.app.data.local.AppDatabase
import com.paycontrol.app.data.preferences.UserPreferencesRepository
import com.paycontrol.app.data.repository.ClientRepository
import com.paycontrol.app.data.repository.FinanceRepository
import com.paycontrol.app.data.repository.SupplierRepository

class PayControlApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val userPreferences: UserPreferencesRepository by lazy {
        UserPreferencesRepository(this)
    }

    val contactsRepository: ContactsRepository by lazy {
        ContactsRepository(contentResolver)
    }

    val financeRepository: FinanceRepository by lazy {
        FinanceRepository(database)
    }

    val supplierRepository: SupplierRepository by lazy {
        SupplierRepository(database.supplierDao())
    }

    val clientRepository: ClientRepository by lazy {
        ClientRepository(database.clientDao())
    }
}
