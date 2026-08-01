package com.paycontrol.app.ui.screens.transactions

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.paycontrol.app.R
import com.paycontrol.app.data.local.entity.AccountEntity
import com.paycontrol.app.data.local.entity.TransactionEntity
import com.paycontrol.app.data.repository.FinanceRepository
import com.paycontrol.app.domain.model.DefaultCategories
import com.paycontrol.app.domain.model.TransactionType
import com.paycontrol.app.domain.util.AppLog
import com.paycontrol.app.domain.util.DateTimeUtils
import com.paycontrol.app.domain.util.Money
import com.paycontrol.app.domain.util.UiErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PendingDeleteTx(
    val id: Long,
    val type: String,
    val category: String,
    val amount: Long
)

data class TransactionFormState(
    val accountId: Long? = null,
    val amountInput: String = "",
    val type: String = TransactionType.EXPENSE,
    val category: String = DefaultCategories.expense.first(),
    val note: String = "",
    val isSaving: Boolean = false,
    val deletingId: Long? = null,
    val pendingDelete: PendingDeleteTx? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val app: Application,
    private val financeRepository: FinanceRepository,
    private val uiErrorMapper: UiErrorMapper
) : ViewModel() {

    val accounts: StateFlow<List<AccountEntity>> = financeRepository
        .observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pagedTransactions: Flow<PagingData<TransactionEntity>> = Pager(
        config = PagingConfig(pageSize = 40, enablePlaceholders = false),
        pagingSourceFactory = { financeRepository.transactionsPagingSource() }
    ).flow.cachedIn(viewModelScope)

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

    fun requestDelete(tx: TransactionEntity) {
        _form.update {
            it.copy(
                pendingDelete = PendingDeleteTx(tx.id, tx.type, tx.category, tx.amount),
                errorMessage = null
            )
        }
    }

    fun dismissDeleteConfirm() {
        _form.update { it.copy(pendingDelete = null) }
    }

    fun ensureDefaultAccount() {
        viewModelScope.launch {
            runCatching { financeRepository.ensureDefaultAccount() }
                .onFailure { error ->
                    AppLog.e(TAG, "Error al preparar cuenta por defecto", error)
                    _form.update {
                        it.copy(errorMessage = error.message ?: app.getString(R.string.msg_tx_prepare_account_failed))
                    }
                }
        }
    }

    fun saveTransaction() {
        val amountCents = Money.parseToCents(_form.value.amountInput)
        val accountId = _form.value.accountId ?: accounts.value.firstOrNull()?.id
        val type = _form.value.type
        val category = _form.value.category
        val note = _form.value.note

        if (accountId == null) {
            _form.update { it.copy(errorMessage = app.getString(R.string.msg_select_account)) }
            return
        }
        if (amountCents == null || amountCents <= 0L) {
            _form.update { it.copy(errorMessage = app.getString(R.string.msg_invalid_amount)) }
            return
        }

        var claimed = false
        _form.update { state ->
            if (state.isSaving) state
            else {
                claimed = true
                state.copy(isSaving = true, errorMessage = null, successMessage = null)
            }
        }
        if (!claimed) return

        viewModelScope.launch {
            runCatching {
                financeRepository.registerTransaction(
                    accountId = accountId,
                    amountCents = amountCents,
                    type = type,
                    category = category,
                    date = DateTimeUtils.nowEpochMillis(),
                    note = note
                )
            }.onSuccess {
                _form.update {
                    TransactionFormState(
                        accountId = accountId,
                        type = type,
                        category = category,
                        successMessage = app.getString(R.string.msg_tx_saved)
                    )
                }
            }.onFailure { error ->
                AppLog.e(TAG, "Error al guardar movimiento", error)
                _form.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = friendlyError(error, app.getString(R.string.msg_tx_save_failed))
                    )
                }
            }
        }
    }

    fun confirmDeletePending() {
        val pending = _form.value.pendingDelete ?: return
        _form.update { it.copy(pendingDelete = null) }
        deleteTransaction(pending.id)
    }

    fun deleteTransaction(id: Long) {
        var claimed = false
        _form.update { state ->
            if (state.deletingId != null) state
            else {
                claimed = true
                state.copy(deletingId = id, errorMessage = null, successMessage = null)
            }
        }
        if (!claimed) return

        viewModelScope.launch {
            runCatching {
                financeRepository.deleteTransaction(id)
            }.onSuccess {
                _form.update {
                    it.copy(
                        deletingId = null,
                        successMessage = app.getString(R.string.msg_tx_deleted),
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                AppLog.e(TAG, "Error al eliminar movimiento id=$id", error)
                _form.update {
                    it.copy(
                        deletingId = null,
                        errorMessage = friendlyError(error, app.getString(R.string.msg_tx_delete_failed))
                    )
                }
            }
        }
    }

    private fun friendlyError(error: Throwable, fallback: String): String =
        uiErrorMapper.map(error, fallback)

    companion object {
        private const val TAG = "TransactionsVM"
    }
}
