package com.paycontrol.app.ui.screens.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paycontrol.app.data.local.entity.AccountEntity
import com.paycontrol.app.data.repository.FinanceRepository
import com.paycontrol.app.domain.model.AccountType
import com.paycontrol.app.domain.util.AppLog
import com.paycontrol.app.domain.util.Money
import com.paycontrol.app.domain.util.UiErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountsUiState(
    val name: String = "",
    val type: String = AccountType.CASH,
    val initialBalanceInput: String = "",
    val selectedAccountId: Long? = null,
    val editName: String = "",
    val editType: String = AccountType.CASH,
    val transferFromAccountId: Long? = null,
    val transferToAccountId: Long? = null,
    val transferAmountInput: String = "",
    val showDeleteConfirm: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val uiErrorMapper: UiErrorMapper
) : ViewModel() {

    val accounts: StateFlow<List<AccountEntity>> = financeRepository
        .observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { financeRepository.ensureDefaultAccount() }
                .onFailure { error ->
                    AppLog.e(TAG, "Error al preparar cuenta por defecto", error)
                }
        }
        viewModelScope.launch {
            accounts.collect { list ->
                _uiState.update { state ->
                    ensureTransferDefaults(state, list)
                }
            }
        }
    }

    fun onNameChange(value: String) =
        _uiState.update { it.copy(name = value, errorMessage = null, successMessage = null) }

    fun onTypeChange(value: String) =
        _uiState.update { it.copy(type = value, errorMessage = null) }

    fun onInitialBalanceChange(value: String) =
        _uiState.update { it.copy(initialBalanceInput = value, errorMessage = null) }

    fun onEditNameChange(value: String) =
        _uiState.update { it.copy(editName = value, errorMessage = null, successMessage = null) }

    fun onEditTypeChange(value: String) =
        _uiState.update { it.copy(editType = value, errorMessage = null) }

    fun onTransferFromAccountSelected(id: Long) {
        _uiState.update { state ->
            val toId = when {
                state.transferToAccountId != null && state.transferToAccountId != id ->
                    state.transferToAccountId
                else -> accounts.value.firstOrNull { it.id != id }?.id
            }
            state.copy(
                transferFromAccountId = id,
                transferToAccountId = toId,
                errorMessage = null
            )
        }
    }

    fun onTransferToAccountSelected(id: Long) {
        _uiState.update { state ->
            when {
                id != state.transferFromAccountId ->
                    state.copy(transferToAccountId = id, errorMessage = null)
                // Auto-picker race: keep an already-distinct destino
                state.transferToAccountId != null && state.transferToAccountId != id ->
                    state
                else -> {
                    val other = accounts.value.firstOrNull { it.id != id }?.id
                    state.copy(transferToAccountId = other ?: id, errorMessage = null)
                }
            }
        }
    }

    fun onTransferAmountChange(value: String) =
        _uiState.update { it.copy(transferAmountInput = value, errorMessage = null) }

    fun onAccountSelected(id: Long) {
        val account = accounts.value.firstOrNull { it.id == id } ?: return
        _uiState.update {
            it.copy(
                selectedAccountId = id,
                editName = account.name,
                editType = account.type,
                showDeleteConfirm = false,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedAccountId = null,
                editName = "",
                editType = AccountType.CASH,
                showDeleteConfirm = false
            )
        }
    }

    fun requestDelete() {
        if (_uiState.value.selectedAccountId == null) return
        _uiState.update { it.copy(showDeleteConfirm = true, errorMessage = null) }
    }

    fun dismissDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = false) }
    }

    fun createAccount() {
        val state = _uiState.value
        if (state.isSaving) return
        val balance = if (state.initialBalanceInput.isBlank()) {
            0L
        } else {
            Money.parseToCents(state.initialBalanceInput)
        }
        if (balance == null) {
            _uiState.update { it.copy(errorMessage = "Saldo inicial inválido") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            runCatching {
                financeRepository.createAccount(state.name, state.type, balance)
            }.onSuccess {
                _uiState.update {
                    ensureTransferDefaults(
                        AccountsUiState(
                            selectedAccountId = state.selectedAccountId,
                            editName = state.editName,
                            editType = state.editType,
                            transferFromAccountId = state.transferFromAccountId,
                            transferToAccountId = state.transferToAccountId,
                            successMessage = "Cuenta creada"
                        ),
                        accounts.value
                    )
                }
            }.onFailure { error ->
                AppLog.e(TAG, "Error al crear cuenta", error)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = friendlyError(error, "No se pudo crear la cuenta")
                    )
                }
            }
        }
    }

    fun updateAccount() {
        val state = _uiState.value
        val id = state.selectedAccountId ?: run {
            _uiState.update { it.copy(errorMessage = "Selecciona una cuenta de la lista") }
            return
        }
        if (state.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            runCatching {
                financeRepository.updateAccount(id, state.editName, state.editType)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        successMessage = "Cuenta actualizada",
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                AppLog.e(TAG, "Error al actualizar cuenta", error)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = friendlyError(error, "No se pudo actualizar la cuenta")
                    )
                }
            }
        }
    }

    fun confirmDelete() {
        val state = _uiState.value
        val id = state.selectedAccountId ?: return
        if (state.isSaving) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isSaving = true, showDeleteConfirm = false, errorMessage = null, successMessage = null)
            }
            runCatching {
                financeRepository.deleteAccount(id)
            }.onSuccess {
                _uiState.update {
                    ensureTransferDefaults(
                        AccountsUiState(successMessage = "Cuenta eliminada"),
                        accounts.value.filter { account -> account.id != id }
                    )
                }
            }.onFailure { error ->
                AppLog.e(TAG, "Error al eliminar cuenta", error)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = friendlyError(error, "No se pudo eliminar la cuenta")
                    )
                }
            }
        }
    }

    fun transferBetweenAccounts() {
        val state = _uiState.value
        val fromId = state.transferFromAccountId
        val toId = state.transferToAccountId
        if (fromId == null || toId == null) {
            _uiState.update { it.copy(errorMessage = "Selecciona cuenta de origen y destino") }
            return
        }
        val amount = Money.parseToCents(state.transferAmountInput)
        if (amount == null || amount <= 0L) {
            _uiState.update { it.copy(errorMessage = "Monto inválido") }
            return
        }

        var claimed = false
        _uiState.update { current ->
            if (current.isSaving) current
            else {
                claimed = true
                current.copy(isSaving = true, errorMessage = null, successMessage = null)
            }
        }
        if (!claimed) return

        viewModelScope.launch {
            runCatching {
                financeRepository.transferBetweenAccounts(
                    fromAccountId = fromId,
                    toAccountId = toId,
                    amountCents = amount
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        transferAmountInput = "",
                        successMessage = "Transferencia realizada",
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                AppLog.e(TAG, "Error al transferir entre cuentas", error)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = friendlyError(error, "No se pudo realizar la transferencia")
                    )
                }
            }
        }
    }

    private fun ensureTransferDefaults(
        state: AccountsUiState,
        list: List<AccountEntity>
    ): AccountsUiState {
        val fromId = state.transferFromAccountId?.takeIf { id -> list.any { it.id == id } }
        val toId = state.transferToAccountId?.takeIf { id -> list.any { it.id == id } }
        if (fromId != null && toId != null && fromId != toId) return state
        return when {
            list.size >= 2 -> {
                val from = fromId ?: list[0].id
                val to = list.firstOrNull { it.id != from }?.id ?: list[1].id
                state.copy(transferFromAccountId = from, transferToAccountId = to)
            }
            list.size == 1 -> state.copy(
                transferFromAccountId = list[0].id,
                transferToAccountId = null
            )
            else -> state.copy(
                transferFromAccountId = null,
                transferToAccountId = null
            )
        }
    }

    private fun friendlyError(error: Throwable, fallback: String): String =
        uiErrorMapper.map(error, fallback)

    companion object {
        private const val TAG = "AccountsVM"
    }
}
