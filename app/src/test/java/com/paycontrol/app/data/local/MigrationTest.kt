package com.paycontrol.app.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Verifica MIGRATION_1_2 y MIGRATION_2_3: columnas/índices y datos preservados.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class MigrationTest {

    @Test
    fun migrate1To3_preservesDataAndAddsColumnsIndexes() {
        val context = RuntimeEnvironment.getApplication()
        val dbName = "migration-sql-test.db"
        context.deleteDatabase(dbName)

        createV1Database(context, dbName).use { db ->
            db.execSQL(
                "INSERT INTO accounts (id, name, balance, type) VALUES (1, 'Caja', 2500, 'Efectivo')"
            )
            db.execSQL(
                """
                INSERT INTO transactions (id, accountId, amount, type, category, date, note)
                VALUES (1, 1, 2500, 'INGRESO', 'Ventas', 1700000000000, 'seed')
                """.trimIndent()
            )

            AppDatabase.MIGRATION_1_2.migrate(db)
            AppDatabase.MIGRATION_2_3.migrate(db)

            db.query("SELECT name, balance FROM accounts WHERE id = 1").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getString(0)).isEqualTo("Caja")
                assertThat(cursor.getLong(1)).isEqualTo(2500L)
            }
            db.query("SELECT amount, note FROM transactions WHERE id = 1").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getLong(0)).isEqualTo(2500L)
                assertThat(cursor.getString(1)).isEqualTo("seed")
            }

            val columns = mutableSetOf<String>()
            db.query("PRAGMA table_info(transactions)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(nameIndex))
                }
            }
            assertThat(columns).containsAtLeast(
                "transferGroupId",
                "transferIsOutbound",
                "relatedClientId",
                "relatedSupplierId"
            )

            val indexes = mutableSetOf<String>()
            db.query("PRAGMA index_list(transactions)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    indexes.add(cursor.getString(nameIndex))
                }
            }
            assertThat(indexes).containsAtLeast(
                "index_transactions_transferGroupId",
                "index_transactions_relatedClientId",
                "index_transactions_relatedSupplierId",
                "index_transactions_date_account_type",
                "index_transactions_date_type"
            )
        }

        context.deleteDatabase(dbName)
    }

    private fun createV1Database(context: Context, name: String): SupportSQLiteDatabase {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS accounts (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT NOT NULL,
                            balance INTEGER NOT NULL,
                            type TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS index_accounts_name ON accounts(name)"
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS transactions (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            accountId INTEGER NOT NULL,
                            amount INTEGER NOT NULL,
                            type TEXT NOT NULL,
                            category TEXT NOT NULL,
                            date INTEGER NOT NULL,
                            note TEXT NOT NULL,
                            FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_transactions_accountId ON transactions(accountId)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_transactions_date ON transactions(date)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_transactions_type ON transactions(type)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_transactions_category ON transactions(category)"
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS clients (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT NOT NULL,
                            phone TEXT NOT NULL,
                            totalDebt INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS index_clients_name ON clients(name)"
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS suppliers (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT NOT NULL,
                            phone TEXT NOT NULL,
                            totalPaid INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS index_suppliers_name ON suppliers(name)"
                    )
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) = Unit
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
    }
}
