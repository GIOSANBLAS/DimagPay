package com.paycontrol.app.data.repository

import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.paycontrol.app.data.local.AppDatabase
import com.paycontrol.app.data.local.entity.TransactionEntity
import com.paycontrol.app.domain.model.TransactionType
import com.paycontrol.app.domain.util.DateTimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class ClientRepositoryTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var db: AppDatabase
    private lateinit var clients: ClientRepository

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        clients = ClientRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun createClient_withInitialDebt() = runTest(dispatcher) {
        val id = clients.createClient("Ana", phone = "555", initialDebtCents = 12_500L)
        val client = db.clientDao().getById(id)!!
        assertThat(client.name).isEqualTo("Ana")
        assertThat(client.phone).isEqualTo("555")
        assertThat(client.totalDebt).isEqualTo(12_500L)
    }

    @Test
    fun addDebt_andUpdateClient() = runTest(dispatcher) {
        val id = clients.createClient("Luis", initialDebtCents = 1_000L)
        clients.addDebt(id, 500L)
        clients.updateClient(id, "Luis Pérez", "123")

        val client = db.clientDao().getById(id)!!
        assertThat(client.totalDebt).isEqualTo(1_500L)
        assertThat(client.name).isEqualTo("Luis Pérez")
        assertThat(client.phone).isEqualTo("123")
    }

    @Test
    fun delete_clearsRelatedTransactionLinks() = runTest(dispatcher) {
        val accountId = db.accountDao().insert(
            com.paycontrol.app.data.local.entity.AccountEntity(
                name = "Caja",
                balance = 0L,
                type = "Efectivo"
            )
        )
        val clientId = clients.createClient("María", initialDebtCents = 2_000L)
        val txId = db.transactionDao().insert(
            TransactionEntity(
                accountId = accountId,
                amount = 500L,
                type = TransactionType.INCOME,
                category = "Cobranza",
                date = DateTimeUtils.nowEpochMillis(),
                note = "abono",
                relatedClientId = clientId
            )
        )

        clients.deleteById(clientId)

        assertThat(db.clientDao().getById(clientId)).isNull()
        assertThat(db.transactionDao().getById(txId)!!.relatedClientId).isNull()
    }
}
