package com.paycontrol.app.ui.screens.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paycontrol.app.data.preferences.UserPreferencesRepository
import com.paycontrol.app.domain.util.AppLog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PinSettingsUiState(
    val currentPin: String = "",
    val newPin: String = "",
    val confirmPin: String = "",
    val disablePin: String = "",
    val message: String? = null,
    val messageIsError: Boolean = false,
    val isBusy: Boolean = false
)

@HiltViewModel
class PinSettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    val pinEnabled: StateFlow<Boolean> = preferences.pinEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), preferences.pinEnabled.value)

    private val _ui = MutableStateFlow(PinSettingsUiState())
    val ui: StateFlow<PinSettingsUiState> = _ui.asStateFlow()

    fun onCurrentPinChange(value: String) {
        _ui.update { it.copy(currentPin = digitsOnly(value), message = null) }
    }

    fun onNewPinChange(value: String) {
        _ui.update { it.copy(newPin = digitsOnly(value), message = null) }
    }

    fun onConfirmPinChange(value: String) {
        _ui.update { it.copy(confirmPin = digitsOnly(value), message = null) }
    }

    fun onDisablePinChange(value: String) {
        _ui.update { it.copy(disablePin = digitsOnly(value), message = null) }
    }

    fun enablePin() {
        val state = _ui.value
        val validation = validateNewPin(state.newPin, state.confirmPin)
        if (validation != null) {
            showError(validation)
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(isBusy = true, message = null) }
            runCatching { preferences.setPin(state.newPin) }
                .onSuccess {
                    _ui.value = PinSettingsUiState(
                        message = "PIN activado correctamente",
                        messageIsError = false
                    )
                }
                .onFailure { error ->
                    AppLog.e(TAG, "Error al activar PIN", error)
                    showError("No se pudo activar el PIN")
                }
        }
    }

    fun changePin() {
        val state = _ui.value
        if (!UserPreferencesRepository.isValidPinFormat(state.currentPin)) {
            showError("Introduce tu PIN actual")
            return
        }
        val validation = validateNewPin(state.newPin, state.confirmPin)
        if (validation != null) {
            showError(validation)
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(isBusy = true, message = null) }
            val ok = runCatching {
                preferences.changePin(state.currentPin, state.newPin)
            }.onFailure { error ->
                AppLog.e(TAG, "Error al cambiar PIN", error)
            }.getOrDefault(false)
            if (ok) {
                _ui.value = PinSettingsUiState(
                    message = "PIN actualizado",
                    messageIsError = false
                )
            } else {
                showError("PIN actual incorrecto")
            }
        }
    }

    fun disablePin() {
        val pin = _ui.value.disablePin
        if (!UserPreferencesRepository.isValidPinFormat(pin)) {
            showError("Introduce tu PIN actual para desactivar")
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(isBusy = true, message = null) }
            val ok = runCatching { preferences.disablePin(pin) }
                .onFailure { error ->
                    AppLog.e(TAG, "Error al desactivar PIN", error)
                }
                .getOrDefault(false)
            if (ok) {
                _ui.value = PinSettingsUiState(
                    message = "Bloqueo por PIN desactivado",
                    messageIsError = false
                )
            } else {
                showError("PIN incorrecto")
            }
        }
    }

    private fun validateNewPin(newPin: String, confirm: String): String? {
        if (!UserPreferencesRepository.isValidPinFormat(newPin)) {
            return "El PIN debe tener entre 4 y 8 dígitos"
        }
        if (newPin != confirm) {
            return "Los PIN no coinciden"
        }
        return null
    }

    private fun showError(message: String) {
        _ui.update {
            it.copy(isBusy = false, message = message, messageIsError = true)
        }
    }

    private fun digitsOnly(value: String): String =
        value.filter { it.isDigit() }.take(UserPreferencesRepository.PIN_MAX_LENGTH)

    companion object {
        private const val TAG = "PinSettingsVM"
    }
}
