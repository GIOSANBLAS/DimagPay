package com.paycontrol.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        SupplierEntity::class,
        ClientEntity::class
    ],
    version = 3,
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

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN transferGroupId INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN transferIsOutbound INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN relatedClientId INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN relatedSupplierId INTEGER")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_transferGroupId " +
                        "ON transactions(transferGroupId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_relatedClientId " +
                        "ON transactions(relatedClientId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_relatedSupplierId " +
                        "ON transactions(relatedSupplierId)"
                )
            }
        }

        /** Índices compuestos para filtros de reportes + fechas UTC documentadas. */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_date_account_type " +
                        "ON transactions(date, accountId, type)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_date_type " +
                        "ON transactions(date, type)"
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(context: Context): AppDatabase {
            // Legacy plaintext DB (pre-SQLCipher). Solo se borra si no hay DB segura aún
            // para no destruir datos en cada arranque si el usuario aún no migró.
            val secureExists = context.getDatabasePath(NAME).exists()
            if (!secureExists) {
                context.deleteDatabase("paycontrol.db")
            }

            val passphrase = SecureStore.databasePassphrase(context)
            val factory = SupportFactory(passphrase)

            return Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }
    }
}
