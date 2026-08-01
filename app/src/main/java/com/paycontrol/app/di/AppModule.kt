package com.paycontrol.app.di

import android.content.Context
import com.paycontrol.app.data.backup.BackupManager
import com.paycontrol.app.data.contacts.ContactsRepository
import com.paycontrol.app.data.local.AppDatabase
import com.paycontrol.app.data.preferences.UserPreferencesRepository
import com.paycontrol.app.data.repository.ClientRepository
import com.paycontrol.app.data.repository.FinanceRepository
import com.paycontrol.app.data.repository.SupplierRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferencesRepository =
        UserPreferencesRepository(context)

    @Provides
    @Singleton
    fun provideContactsRepository(@ApplicationContext context: Context): ContactsRepository =
        ContactsRepository(context.contentResolver)

    @Provides
    @Singleton
    fun provideFinanceRepository(
        @ApplicationContext context: Context,
        database: AppDatabase
    ): FinanceRepository = FinanceRepository(context, database)

    @Provides
    @Singleton
    fun provideClientRepository(database: AppDatabase): ClientRepository =
        ClientRepository(database)

    @Provides
    @Singleton
    fun provideSupplierRepository(database: AppDatabase): SupplierRepository =
        SupplierRepository(database)

    @Provides
    @Singleton
    fun provideBackupManager(
        @ApplicationContext context: Context,
        database: AppDatabase
    ): BackupManager = BackupManager(context, database)
}

/**
 * Acceso a dependencias desde componentes no Hilt (widget, receivers).
 */
@dagger.hilt.EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun financeRepository(): FinanceRepository
    fun userPreferences(): UserPreferencesRepository
    fun database(): AppDatabase
}
