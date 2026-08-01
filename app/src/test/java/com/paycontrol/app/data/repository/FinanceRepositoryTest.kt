package com.paycontrol.app.data.repository

import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.paycontrol.app.data.local.AppDatabase
import com.paycontrol.app.domain.model.AccountType
import com.paycontrol.app.domain.model.TransactionType
import com.paycontrol.app.domain.util.DateTimeUtils
import com.paycontrol.app.domain.util.DomainStrings
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
class FinanceRepositoryTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var db: AppDatabase
    private lateinit var finance: FinanceRepository
    private lateinit var clients: ClientRepository
    private lateinit var suppliers: SupplierRepository

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val messages = DomainStrings(context)
        finance = FinanceRepository(context, db, messages)
        clients = ClientRepository(db, messages)
        suppliers = SupplierRepository(db, messages)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun createAccount_setsInitialBalance() = runTest(dispatcher) {
        val id = finance.createAccount("Caja", AccountType.CASH, initialBalanceCents = 10_000L)
        val account = db.accountDao().getById(id)!!
        assertThat(account.balance).isEqualTo(10_000L)
        assertThat(account.name).isEqualTo("Caja")
    }

    @Test
    fun registerIncomeAndExpense_updatesBalance() = runTest(dispatcher) {
        val accountId = finance.createAccount("Banco", AccountType.BANK, 5_000L)
        val now = DateTimeUtils.nowEpochMillis()

        finance.registerTransaction(
            accountId = accountId,
            amountCents = 2_000L,
            type = TransactionType.INCOME,
            category = "Ventas",
            date = now
        )
        assertThat(db.accountDao().getById(accountId)!!.balance).isEqualTo(7_000L)

        finance.registerTransaction(
            accountId = accountId,
            amountCents = 1_500L,
            type = TransactionType.EXPENSE,
            category = "Compras",
            date = now
        )
        assertThat(db.accountDao().getById(accountId)!!.balance).isEqualTo(5_500L)
    }

    @Test
    fun transferBetweenAccounts_updatesBothAndSharesGroupId() = runTest(dispatcher) {
        val fromId = finance.createAccount("Origen", AccountType.CASH, 10_000L)
        val toId = finance.createAccount("Destino", AccountType.BANK, 1_000L)

        finance.transferBetweenAccounts(fromId, toId, 3_000L, note = "prueba")

        assertThat(db.accountDao().getById(fromId)!!.balance).isEqualTo(7_000L)
        assertThat(db.accountDao().getById(toId)!!.balance).isEqualTo(4_000L)

        val legs = db.transactionDao().getAll()
            .filter { it.type == TransactionType.TRANSFER }
        assertThat(legs).hasSize(2)
        assertThat(legs[0].transferGroupId).isNotNull()
        assertThat(legs[0].transferGroupId).isEqualTo(legs[1].transferGroupId)
        assertThat(legs.map { it.transferIsOutbound }.toSet())
            .containsExactly(true, false)
    }

    @Test
    fun paySupplier_decrementsAccountAndIncrementsTotalPaid() = runTest(dispatcher) {
        val accountId = finance.createAccount("Caja", AccountType.CASH, 20_000L)
        val supplierId = suppliers.createSupplier("Acme")

        finance.paySupplier(supplierId, accountId, 4_000L)

        assertThat(db.accountDao().getById(accountId)!!.balance).isEqualTo(16_000L)
        assertThat(db.supplierDao().getById(supplierId)!!.totalPaid).isEqualTo(4_000L)
    }

    @Test
    fun receiveClientPayment_incrementsAccountAndReducesDebt() = runTest(dispatcher) {
        val accountId = finance.createAccount("Caja", AccountType.CASH, 1_000L)
        val clientId = clients.createClient("Cliente A", initialDebtCents = 8_000L)

        finance.receiveClientPayment(clientId, accountId, 3_000L)

        assertThat(db.accountDao().getById(accountId)!!.balance).isEqualTo(4_000L)
        assertThat(db.clientDao().getById(clientId)!!.totalDebt).isEqualTo(5_000L)
    }

    @Test
    fun deleteTransaction_revertsBalanceAndRelatedDebt() = runTest(dispatcher) {
        val accountId = finance.createAccount("Caja", AccountType.CASH, 1_000L)
        val clientId = clients.createClient("Cliente B", initialDebtCents = 5_000L)
        val txId = finance.receiveClientPayment(clientId, accountId, 2_000L)

        finance.deleteTransaction(txId)

        assertThat(db.accountDao().getById(accountId)!!.balance).isEqualTo(1_000L)
        assertThat(db.clientDao().getById(clientId)!!.totalDebt).isEqualTo(5_000L)
        assertThat(db.transactionDao().getById(txId)).isNull()
    }

    @Test
    fun deleteTransaction_revertsSupplierTotalPaid() = runTest(dispatcher) {
        val accountId = finance.createAccount("Caja", AccountType.CASH, 10_000L)
        val supplierId = suppliers.createSupplier("Proveedor X")
        val txId = finance.paySupplier(supplierId, accountId, 2_500L)

        finance.deleteTransaction(txId)

        assertThat(db.accountDao().getById(accountId)!!.balance).isEqualTo(10_000L)
        assertThat(db.supplierDao().getById(supplierId)!!.totalPaid).isEqualTo(0L)
    }
}
