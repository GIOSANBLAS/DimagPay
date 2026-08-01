package com.paycontrol.app.ui.screens.clients

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.paycontrol.app.R
import com.paycontrol.app.data.contacts.DeviceContact
import com.paycontrol.app.data.local.entity.AccountEntity
import com.paycontrol.app.data.local.entity.ClientEntity
import com.paycontrol.app.data.repository.ClientRepository
import com.paycontrol.app.data.repository.FinanceRepository
import com.paycontrol.app.domain.util.AppLog
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

data class ClientsUiState(
    val name: String = "",
    val phone: String = "",
    val debtInput: String = "",
    val paymentInput: String = "",
    val addDebtInput: String = "",
    val editName: String = "",
    val editPhone: String = "",
    val selectedClientId: Long? = null,
    val selectedAccountId: Long? = null,
    val isPaying: Boolean = false,
    val isBusy: Boolean = false,
    val showContactPicker: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ClientsViewModel @Inject constructor(
    private val app: Application,
    private val clientRepository: ClientRepository,
    private val financeRepository: FinanceRepository,
    private val uiErrorMapper: UiErrorMapper
) : ViewModel() {

    val clients: StateFlow<List<ClientEntity>> = clientRepository
        .observeClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pagedClients: Flow<PagingData<ClientEntity>> = Pager(
        config = PagingConfig(pageSize = 40, enablePlaceholders = false),
        pagingSourceFactory = { clientRepository.pagingSource() }
    ).flow.cachedIn(viewModelScope)

    val accounts: StateFlow<List<AccountEntity>> = financeRepository
        .observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalReceivables: StateFlow<Long> = clientRepository
        .observeTotalReceivables()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private val _uiState = MutableStateFlow(ClientsUiState())
    val uiState: StateFlow<ClientsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { financeRepository.ensureDefaultAccount() }
                .onFailure { error ->
                    AppLog.e(TAG, "Error al preparar cuenta por defecto", error)
                }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, errorMessage = null) }
    fun onPhoneChange(value: String) = _uiState.update { it.copy(phone = value) }
    fun onDebtInputChange(value: String) = _uiState.update { it.copy(debtInput = value) }
    fun onPaymentInputChange(value: String) = _uiState.update { it.copy(paymentInput = value) }
    fun onAddDebtInputChange(value: String) = _uiState.update { it.copy(addDebtInput = value) }
    fun onEditNameChange(value: String) = _uiState.update { it.copy(editName = value, errorMessage = null) }
    fun onEditPhoneChange(value: String) = _uiState.update { it.copy(editPhone = value) }
    fun onClientSelected(id: Long) {
        val client = clients.value.firstOrNull { it.id == id }
        _uiState.update {
            it.copy(
                selectedClientId = id,
                editName = client?.name.orEmpty(),
                editPhone = client?.phone.orEmpty(),
                addDebtInput = "",
                errorMessage = null,
                successMessage = null
            )
        }
    }
    fun onAccountSelected(id: Long) = _uiState.update { it.copy(selectedAccountId = id) }
    fun openContactPicker() = _uiState.update { it.copy(showContactPicker = true) }
    fun closeContactPicker() = _uiState.update { it.copy(showContactPicker = false) }

    fun applyContact(contact: DeviceContact) {
        _uiState.update {
            it.copy(
                name = contact.name,
                phone = contact.phone,
                showContactPicker = false,
                errorMessage = null
            )
        }
    }

    fun createClient() {
        val state = _uiState.value
        val debt = if (state.debtInput.isBlank()) 0L else Money.parseToCents(state.debtInput)
        if (debt == null) {
            _uiState.update { it.copy(errorMessage = app.getString(R.string.msg_invalid_initial_debt)) }
            return
        }
        viewModelScope.launch {
            runCatching {
                clientRepository.createClient(state.name, state.phone, debt)
            }.onSuccess {
                _uiState.update {
                    ClientsUiState(
                        selectedAccountId = state.selectedAccountId,
                        successMessage = app.getString(R.string.msg_client_created)
                    )
                }
            }.onFailure { error ->
                AppLog.e(TAG, "Error al crear cliente", error)
                _uiState.update {
                    it.copy(errorMessage = friendlyError(error, app.getString(R.string.msg_client_create_failed)))
                }
            }
        }
    }

    fun applyPayment() {
        val state = _uiState.value
        val clientId = state.selectedClientId
        val accountId = state.selectedAccountId ?: accounts.value.firstOrNull()?.id
        val amount = Money.parseToCents(state.paymentInput)
        if (clientId == null) {
            _uiState.update { it.copy(errorMessage = app.getString(R.string.msg_client_select)) }
            return
        }
        if (accountId == null) {
            _uiState.update { it.copy(errorMessage = app.getString(R.string.msg_client_select_dest_account)) }
            return
        }
        if (amount == null || amount <= 0L) {
            _uiState.update { it.copy(errorMessage = app.getString(R.string.msg_invalid_deposit)) }
            return
        }

        var claimed = false
        _uiState.update { current ->
            if (current.isPaying) current
            else {
                claimed = true
                current.copy(isPaying = true, errorMessage = null, successMessage = null)
            }
        }
        if (!claimed) return

        viewModelScope.launch {
            runCatching {
                financeRepository.receiveClientPayment(clientId, accountId, amount)
            }.onSuccess {
                val clientName = clients.value.firstOrNull { it.id == clientId }?.name ?: "cliente"
                val accountName = accounts.value.firstOrNull { it.id == accountId }?.name ?: "cuenta"
                _uiState.update {
                    it.copy(
                        paymentInput = "",
                        isPaying = false,
                        successMessage = app.getString(R.string.msg_deposit_ok, Money.format(amount), clientName, accountName),
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                AppLog.e(TAG, "Error al aplicar abono de cliente", error)
                _uiState.update {
                    it.copy(
                        isPaying = false,
                        errorMessage = friendlyError(error, app.getString(R.string.msg_deposit_failed))
                    )
                }
            }
        }
    }

    fun addDebt() {
        val state = _uiState.value
        val clientId = state.selectedClientId
        val amount = Money.parseToCents(state.addDebtInput)
        if (clientId == null) {
            _uiState.update { it.copy(errorMessage = app.getString(R.string.msg_client_select)) }
            return
        }
        if (amount == null || amount <= 0L) {
            _uiState.update { it.copy(errorMessage = app.getString(R.string.msg_invalid_debt_amount)) }
            return
        }
        if (state.isBusy) return

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null, successMessage = null) }
            runCatching {
                clientRepository.addDebt(clientId, amount)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        addDebtInput = "",
                        isBusy = false,
                        successMessage = app.getString(R.string.msg_debt_added, Money.format(amount)),
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                AppLog.e(TAG, "Error al agregar deuda de cliente", error)
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        errorMessage = friendlyError(error, app.getString(R.string.msg_debt_add_failed))
                    )
                }
            }
        }
    }

    fun saveClientEdits() {
        val state = _uiState.value
        val clientId = state.selectedClientId
        if (clientId == null) {
            _uiState.update { it.copy(errorMessage = app.getString(R.string.msg_client_select)) }
            return
        }
        if (state.isBusy) return

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null, successMessage = null) }
            runCatching {
                clientRepository.updateClient(clientId, state.editName, state.editPhone)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        successMessage = app.getString(R.string.msg_client_updated),
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                AppLog.e(TAG, "Error al actualizar cliente", error)
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        errorMessage = friendlyError(error, app.getString(R.string.msg_client_update_failed))
                    )
                }
            }
        }
    }

    fun deleteSelectedClient() {
        val state = _uiState.value
        val clientId = state.selectedClientId
        if (clientId == null) {
            _uiState.update { it.copy(errorMessage = app.getString(R.string.msg_client_select)) }
            return
        }
        if (state.isBusy) return

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null, successMessage = null) }
            runCatching {
                clientRepository.deleteById(clientId)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        selectedClientId = null,
                        editName = "",
                        editPhone = "",
                        addDebtInput = "",
                        paymentInput = "",
                        isBusy = false,
                        successMessage = app.getString(R.string.msg_client_deleted),
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                AppLog.e(TAG, "Error al eliminar cliente", error)
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        errorMessage = friendlyError(error, app.getString(R.string.msg_client_delete_failed))
                    )
                }
            }
        }
    }

    private fun friendlyError(error: Throwable, fallback: String): String =
        uiErrorMapper.map(error, fallback)

    companion object {
        private const val TAG = "ClientsVM"
    }
}
