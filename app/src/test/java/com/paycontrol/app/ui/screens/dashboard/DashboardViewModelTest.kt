package com.paycontrol.app.ui.screens.dashboard

import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.paycontrol.app.data.local.AppDatabase
import com.paycontrol.app.data.preferences.UserPreferencesRepository
import com.paycontrol.app.data.repository.ClientRepository
import com.paycontrol.app.data.repository.FinanceRepository
import com.paycontrol.app.domain.model.AccountType
import com.paycontrol.app.domain.model.TransactionType
import com.paycontrol.app.domain.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
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
class DashboardViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var db: AppDatabase
    private lateinit var finance: FinanceRepository
    private lateinit var clients: ClientRepository
    private lateinit var preferences: UserPreferencesRepository
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        finance = FinanceRepository(context, db)
        clients = ClientRepository(db)
        preferences = UserPreferencesRepository(
            context.getSharedPreferences("dashboard_vm_test", 0)
        )
        viewModel = DashboardViewModel(finance, clients, preferences)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_combinesIncomeExpenseBalanceAndDisplayName() = runBlocking {
        // Tiempo real: Room emite en Dispatchers.IO; runTest usaría timeout virtual.
        val collector = launch { viewModel.uiState.collect {} }

        preferences.updateDisplayName("Carla")
        val accountId = finance.createAccount("Caja", AccountType.CASH, 0L)
        val now = DateTimeUtils.nowEpochMillis()
        finance.registerTransaction(
            accountId, 10_000L, TransactionType.INCOME, "Ventas", now
        )
        finance.registerTransaction(
            accountId, 3_000L, TransactionType.EXPENSE, "Compras", now
        )
        clients.createClient("Deudor", initialDebtCents = 2_000L)

        val state = withTimeout(5_000) {
            viewModel.uiState.first {
                it.displayName == "Carla" &&
                    it.totalIncomeCents == 10_000L &&
                    it.totalExpenseCents == 3_000L &&
                    it.accountsBalanceCents == 7_000L &&
                    it.receivablesCents == 2_000L &&
                    it.recentTransactions.isNotEmpty()
            }
        }

        assertThat(state.consolidatedBalanceCents).isEqualTo(7_000L)
        assertThat(state.displayName).isEqualTo("Carla")
        collector.cancel()
        dispatcher.cancelChildren()
    }
}
