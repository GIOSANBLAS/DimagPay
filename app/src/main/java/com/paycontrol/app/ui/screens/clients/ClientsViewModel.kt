package com.paycontrol.app.ui.screens.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paycontrol.app.data.contacts.DeviceContact
import com.paycontrol.app.data.local.entity.AccountEntity
import com.paycontrol.app.data.local.entity.ClientEntity
import com.paycontrol.app.data.repository.ClientRepository
import com.paycontrol.app.data.repository.FinanceRepository
import com.paycontrol.app.domain.util.Money
import com.paycontrol.app.domain.util.UiErrorMapper
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

class ClientsViewModel(
    private val clientRepository: ClientRepository,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    val clients: StateFlow<List<ClientEntity>> = clientRepository
        .observeClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
            _uiState.update { it.copy(errorMessage = "Deuda inicial inválida") }
            return
        }
        viewModelScope.launch {
            runCatching {
                clientRepository.createClient(state.name, state.phone, debt)
            }.onSuccess {
                _uiState.update {
                    ClientsUiState(
                        selectedAccountId = state.selectedAccountId,
                        successMessage = "Cliente creado"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = friendlyError(error, "No se pudo crear el cliente"))
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
            _uiState.update { it.copy(errorMessage = "Selecciona un cliente de la lista") }
            return
        }
        if (accountId == null) {
            _uiState.update { it.copy(errorMessage = "Selecciona una cuenta destino") }
            return
        }
        if (amount == null || amount <= 0L) {
            _uiState.update { it.copy(errorMessage = "Abono inválido") }
            return
        }
        if (state.isPaying) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPaying = true, errorMessage = null, successMessage = null) }
            runCatching {
                financeRepository.receiveClientPayment(clientId, accountId, amount)
            }.onSuccess {
                val clientName = clients.value.firstOrNull { it.id == clientId }?.name ?: "cliente"
                val accountName = accounts.value.firstOrNull { it.id == accountId }?.name ?: "cuenta"
                _uiState.update {
                    it.copy(
                        paymentInput = "",
                        isPaying = false,
                        successMessage = "Abono de ${Money.format(amount)} a $clientName · ingresó en $accountName",
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isPaying = false,
                        errorMessage = friendlyError(error, "No se pudo aplicar el abono")
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
            _uiState.update { it.copy(errorMessage = "Selecciona un cliente de la lista") }
            return
        }
        if (amount == null || amount <= 0L) {
            _uiState.update { it.copy(errorMessage = "Monto de deuda inválido") }
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
                        successMessage = "Deuda agregada: ${Money.format(amount)}",
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        errorMessage = friendlyError(error, "No se pudo agregar la deuda")
                    )
                }
            }
        }
    }

    fun saveClientEdits() {
        val state = _uiState.value
        val clientId = state.selectedClientId
        if (clientId == null) {
            _uiState.update { it.copy(errorMessage = "Selecciona un cliente de la lista") }
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
                        successMessage = "Cliente actualizado",
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        errorMessage = friendlyError(error, "No se pudo actualizar el cliente")
                    )
                }
            }
        }
    }

    fun deleteSelectedClient() {
        val state = _uiState.value
        val clientId = state.selectedClientId
        if (clientId == null) {
            _uiState.update { it.copy(errorMessage = "Selecciona un cliente de la lista") }
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
                        successMessage = "Cliente eliminado",
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        errorMessage = friendlyError(error, "No se pudo eliminar el cliente")
                    )
                }
            }
        }
    }

    private fun friendlyError(error: Throwable, fallback: String): String =
        UiErrorMapper.map(error, fallback)
}
