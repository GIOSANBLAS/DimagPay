package com.paycontrol.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paycontrol.app.data.local.entity.TransactionEntity
import com.paycontrol.app.data.preferences.UserPreferencesRepository
import com.paycontrol.app.data.repository.ClientRepository
import com.paycontrol.app.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val displayName: String = "",
    val totalIncomeCents: Long = 0L,
    val totalExpenseCents: Long = 0L,
    val consolidatedBalanceCents: Long = 0L,
    val accountsBalanceCents: Long = 0L,
    val receivablesCents: Long = 0L,
    val recentTransactions: List<TransactionEntity> = emptyList()
)

class DashboardViewModel(
    financeRepository: FinanceRepository,
    clientRepository: ClientRepository,
    preferences: UserPreferencesRepository
) : ViewModel() {

    private val totals = combine(
        financeRepository.observeTotalIncome(),
        financeRepository.observeTotalExpense(),
        financeRepository.observeConsolidatedBalance(),
        financeRepository.observeAccountsBalance(),
        clientRepository.observeTotalReceivables()
    ) { income, expense, consolidated, accountsBalance, receivables ->
        TotalsSnapshot(income, expense, consolidated, accountsBalance, receivables)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        totals,
        financeRepository.observeRecentTransactions(8),
        preferences.displayName
    ) { snapshot, recent, name ->
        DashboardUiState(
            displayName = name,
            totalIncomeCents = snapshot.income,
            totalExpenseCents = snapshot.expense,
            consolidatedBalanceCents = snapshot.consolidated,
            accountsBalanceCents = snapshot.accountsBalance,
            receivablesCents = snapshot.receivables,
            recentTransactions = recent
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    private data class TotalsSnapshot(
        val income: Long,
        val expense: Long,
        val consolidated: Long,
        val accountsBalance: Long,
        val receivables: Long
    )
}
