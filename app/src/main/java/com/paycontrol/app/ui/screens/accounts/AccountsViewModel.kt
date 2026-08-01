package com.paycontrol.app.ui.screens.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paycontrol.app.data.local.entity.AccountEntity
import com.paycontrol.app.data.repository.FinanceRepository
import com.paycontrol.app.domain.model.AccountType
import com.paycontrol.app.domain.util.Money
import com.paycontrol.app.domain.util.UiErrorMapper
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
    val showDeleteConfirm: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AccountsViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    val accounts: StateFlow<List<AccountEntity>> = financeRepository
        .observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { financeRepository.ensureDefaultAccount() }
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
                    AccountsUiState(
                        selectedAccountId = state.selectedAccountId,
                        editName = state.editName,
                        editType = state.editType,
                        successMessage = "Cuenta creada"
                    )
                }
            }.onFailure { error ->
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
                    AccountsUiState(successMessage = "Cuenta eliminada")
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = friendlyError(error, "No se pudo eliminar la cuenta")
                    )
                }
            }
        }
    }

    private fun friendlyError(error: Throwable, fallback: String): String =
        UiErrorMapper.map(error, fallback)
}