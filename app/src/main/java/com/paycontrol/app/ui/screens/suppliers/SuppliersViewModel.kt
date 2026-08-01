package com.paycontrol.app.ui.screens.suppliers

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
import com.paycontrol.app.data.local.entity.SupplierEntity
import com.paycontrol.app.data.repository.FinanceRepository
import com.paycontrol.app.data.repository.SupplierRepository
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

data class SuppliersUiState(
    val name: String = "",
    val phone: String = "",
    val paymentAmount: String = "",
    val editName: String = "",
    val editPhone: String = "",
    val selectedSupplierId: Long? = null,
    val selectedAccountId: Long? = null,
    val isPaying: Boolean = false,
    val isBusy: Boolean = false,
    val showContactPicker: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class SuppliersViewModel @Inject constructor(
    private val app: Application,
    private val supplierRepository: SupplierRepository,
    private val financeRepository: FinanceRepository,
    private val uiErrorMapper: UiErrorMapper
) : ViewModel() {

    val suppliers: StateFlow<List<SupplierEntity>> = supplierRepository
        .observeSuppliers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pagedSuppliers: Flow<PagingData<SupplierEntity>> = Pager(
        config = PagingConfig(pageSize = 40, enablePlaceholders = false),
        pagingSourceFactory = { supplierRepository.pagingSource() }
    ).flow.cachedIn(viewModelScope)

    val accounts: StateFlow<List<AccountEntity>> = financeRepository
        .observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(SuppliersUiState())
    val uiState: StateFlow<SuppliersUiState> = _uiState.asStateFlow()

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
    fun onPaymentAmountChange(value: String) = _uiState.update { it.copy(paymentAmount = value) }
    fun onEditNameChange(value: String) = _uiState.update { it.copy(editName = value, errorMessage = null) }
    fun onEditPhoneChange(value: String) = _uiState.update { it.copy(editPhone = value) }
    fun onSupplierSelected(id: Long) {
        val supplier = suppliers.value.firstOrNull { it.id == id }
        _uiState.update {
            it.copy(
                selectedSupplierId = id,
                editName = supplier?.name.orEmpty(),
                editPhone = supplier?.phone.orEmpty(),
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

    fun createSupplier() {
        val state = _uiState.value
        viewModelScope.launch {
            runCatching {
                supplierRepository.createSupplier(state.name, state.phone)
            }.onSuccess {
                _uiState.update {
                    SuppliersUiState(
                        selectedAccountId = state.selectedAccountId,
                        successMessage = app.getString(R.string.msg_supplier_created)
                    )
                }
            }.onFailure { error ->
                AppLog.e(TAG, "Error al crear proveedor", error)
                _uiState.update {
                    it.copy(errorMessage = friendlyError(error, app.getString(R.string.msg_supplier_create_failed)))
                }
            }
        }
    }

    fun registerPayment() {
        val state = _uiState.value
        val supplierId = state.selectedSupplierId
        val accountId = state.selectedAccountId ?: accounts.value.firstOrNull()?.id
        val amount = Money.parseToCents(state.paymentAmount)
        if (supplierId == null) {
            _uiState.update { it.copy(errorMessage = app.getString(R.string.msg_supplier_select)) }
            return
        }
        if (accountId == null) {
            _uiState.update { it.copy(errorMessage = app.getString(R.string.msg_supplier_select_account)) }
            return
        }
        if (amount == null || amount <= 0L) {
            _uiState.update { it.copy(errorMessage = app.getString(R.string.msg_invalid_payment)) }
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
                financeRepository.paySupplier(supplierId, accountId, amount)
            }.onSuccess {
                val supplierName = suppliers.value.firstOrNull { it.id == supplierId }?.name ?: "proveedor"
                val accountName = accounts.value.firstOrNull { it.id == accountId }?.name ?: "cuenta"
                _uiState.update {
                    it.copy(
                        paymentAmount = "",
                        isPaying = false,
                        successMessage = app.getString(R.string.msg_payment_ok, Money.format(amount), supplierName, accountName),
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                AppLog.e(TAG, "Error al registrar pago a proveedor", error)
                _uiState.update {
                    it.copy(
                        isPaying = false,
                        errorMessage = friendlyError(error, app.getString(R.string.msg_payment_failed))
                    )
                }
            }
        }
    }

    fun saveSupplierEdits() {
        val state = _uiState.value
        val supplierId = state.selectedSupplierId
        if (supplierId == null) {
            _uiState.update { it.copy(errorMessage = app.getString(R.string.msg_supplier_select)) }
            return
        }
        if (state.isBusy) return

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null, successMessage = null) }
            runCatching {
                supplierRepository.updateSupplier(supplierId, state.editName, state.editPhone)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        successMessage = app.getString(R.string.msg_supplier_updated),
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                AppLog.e(TAG, "Error al actualizar proveedor", error)
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        errorMessage = friendlyError(error, app.getString(R.string.msg_supplier_update_failed))
                    )
                }
            }
        }
    }

    fun deleteSelectedSupplier() {
        val state = _uiState.value
        val supplierId = state.selectedSupplierId
        if (supplierId == null) {
            _uiState.update { it.copy(errorMessage = app.getString(R.string.msg_supplier_select)) }
            return
        }
        if (state.isBusy) return

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null, successMessage = null) }
            runCatching {
                supplierRepository.deleteById(supplierId)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        selectedSupplierId = null,
                        editName = "",
                        editPhone = "",
                        paymentAmount = "",
                        isBusy = false,
                        successMessage = app.getString(R.string.msg_supplier_deleted),
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                AppLog.e(TAG, "Error al eliminar proveedor", error)
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        errorMessage = friendlyError(error, app.getString(R.string.msg_supplier_delete_failed))
                    )
                }
            }
        }
    }

    private fun friendlyError(error: Throwable, fallback: String): String =
        uiErrorMapper.map(error, fallback)

    companion object {
        private const val TAG = "SuppliersVM"
    }
}
