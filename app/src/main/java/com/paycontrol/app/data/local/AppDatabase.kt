package com.paycontrol.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.paycontrol.app.data.local.dao.AccountDao
import com.paycontrol.app.data.local.dao.ClientDao
import com.paycontrol.app.data.local.dao.LedgerDao
import com.paycontrol.app.data.local.dao.SupplierDao
import com.paycontrol.app.data.local.dao.TransactionDao
import com.paycontrol.app.data.local.entity.AccountEntity
import com.paycontrol.app.data.local.entity.ClientEntity
import com.paycontrol.app.data.local.entity.SupplierEntity
import com.paycontrol.app.data.local.entity.TransactionEntity
import com.paycontrol.app.data.security.SecureStore
import net.sqlcipher.database.SupportFactory

/**
 * Room database (SQLCipher).
 *
 * exportSchema = false for now. When enabling schema export for CI migration checks,
 * set exportSchema = true and configure room.schemaLocation in Gradle.
 *
 * Prefer explicit [androidx.room.migration.Migration] objects over destructive fallbacks.
 * Keep version = 1 unless the schema actually changes; then bump version and add a Migration.
 */
@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        SupplierEntity::class,
        ClientEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun supplierDao(): SupplierDao
    abstract fun clientDao(): ClientDao
    abstract fun ledgerDao(): LedgerDao

    companion object {
        const val NAME = "paycontrol_secure.db"

        @Volatile
        private var instance: AppDatabase? = null

        // Placeholder for future migrations (example):
        // private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
        //     override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        //         // db.execSQL("ALTER TABLE ...")
        //     }
        // }

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(context: Context): AppDatabase {
            // One-time cleanup of the old plaintext database from early builds.
            context.deleteDatabase("paycontrol.db")

            val passphrase = SecureStore.databasePassphrase(context)
            val factory = SupportFactory(passphrase)

            return Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                .openHelperFactory(factory)
                // No destructive wipe: schema changes must ship with explicit Migration objects.
                // .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
