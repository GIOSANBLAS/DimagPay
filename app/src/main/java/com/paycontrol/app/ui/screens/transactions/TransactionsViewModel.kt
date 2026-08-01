package com.paycontrol.app.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paycontrol.app.data.local.entity.AccountEntity
import com.paycontrol.app.data.local.entity.TransactionEntity
import com.paycontrol.app.data.repository.FinanceRepository
import com.paycontrol.app.domain.model.DefaultCategories
import com.paycontrol.app.domain.model.TransactionType
import com.paycontrol.app.domain.util.Money
import com.paycontrol.app.domain.util.UiErrorMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionFormState(
    val accountId: Long? = null,
    val amountInput: String = "",
    val type: String = TransactionType.EXPENSE,
    val category: String = DefaultCategories.expense.first(),
    val note: String = "",
    val isSaving: Boolean = false,
    val deletingId: Long? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class TransactionsViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    val accounts: StateFlow<List<AccountEntity>> = financeRepository
        .observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = financeRepository
        .observeAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _form = MutableStateFlow(TransactionFormState())
    val form: StateFlow<TransactionFormState> = _form.asStateFlow()

    fun onAmountChange(value: String) {
        _form.update { it.copy(amountInput = value, errorMessage = null) }
    }

    fun onTypeChange(type: String) {
        val categories = if (type == TransactionType.INCOME) {
            DefaultCategories.income
        } else {
            DefaultCategories.expense
        }
        _form.update {
            it.copy(
                type = type,
                category = categories.first(),
                errorMessage = null
            )
        }
    }

    fun onCategoryChange(category: String) {
        _form.update { it.copy(category = category) }
    }

    fun onAccountSelected(accountId: Long) {
        _form.update { it.copy(accountId = accountId, errorMessage = null) }
    }

    fun onNoteChange(note: String) {
        _form.update { it.copy(note = note.take(500)) }
    }

    fun ensureDefaultAccount() {
        viewModelScope.launch {
            runCatching { financeRepository.ensureDefaultAccount() }
                .onFailure { error ->
                    _form.update {
                        it.copy(errorMessage = error.message ?: "No se pudo preparar la cuenta")
                    }
                }
        }
    }

    fun saveTransaction() {
        val current = _form.value
        val accountId = current.accountId ?: accounts.value.firstOrNull()?.id
        val amountCents = Money.parseToCents(current.amountInput)

        if (accountId == null) {
            _form.update { it.copy(errorMessage = "Selecciona una cuenta") }
            return
        }
        if (amountCents == null || amountCents <= 0L) {
            _form.update { it.copy(errorMessage = "Monto inválido") }
            return
        }

        viewModelScope.launch {
            _form.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            runCatching {
                financeRepository.registerTransaction(
                    accountId = accountId,
                    amountCents = amountCents,
                    type = current.type,
                    category = current.category,
                    date = System.currentTimeMillis(),
                    note = current.note
                )
            }.onSuccess {
                _form.update {
                    TransactionFormState(
                        accountId = accountId,
                        type = current.type,
                        category = current.category,
                        successMessage = "Movimiento registrado"
                    )
                }
            }.onFailure { error ->
                _form.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = friendlyError(error, "No se pudo guardar el movimiento")
                    )
                }
            }
        }
    }

    fun deleteTransaction(id: Long) {
        if (_form.value.deletingId != null) return
        viewModelScope.launch {
            _form.update {
                it.copy(deletingId = id, errorMessage = null, successMessage = null)
            }
            runCatching {
                financeRepository.deleteTransaction(id)
            }.onSuccess {
                _form.update {
                    it.copy(
                        deletingId = null,
                        successMessage = "Movimiento eliminado",
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _form.update {
                    it.copy(
                        deletingId = null,
                        errorMessage = friendlyError(error, "No se pudo eliminar el movimiento")
                    )
                }
            }
        }
    }

    private fun friendlyError(error: Throwable, fallback: String): String =
        UiErrorMapper.map(error, fallback)
}
