package com.paycontrol.app.ui.screens.lock

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paycontrol.app.R
import com.paycontrol.app.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LockUiState(
    val pinInput: String = "",
    val error: String? = null,
    val isChecking: Boolean = false
)

/**
 * Estado de desbloqueo de sesión en memoria (se reinicia al matar el proceso).
 */
@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val app: Application,
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    val pinEnabled: StateFlow<Boolean> = preferences.pinEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, preferences.pinEnabled.value)

    // Unlocked for this process if PIN was off at ViewModel creation (enabling PIN mid-session
    // does not force an immediate lock; next cold start will).
    private val _sessionUnlocked = MutableStateFlow(!preferences.pinEnabled.value)
    val sessionUnlocked: StateFlow<Boolean> = _sessionUnlocked.asStateFlow()

    private val _lockUi = MutableStateFlow(LockUiState())
    val lockUi: StateFlow<LockUiState> = _lockUi.asStateFlow()

    fun onPinInputChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(UserPreferencesRepository.PIN_MAX_LENGTH)
        _lockUi.update { it.copy(pinInput = digits, error = null) }
    }

    fun unlockWithPin() {
        val pin = _lockUi.value.pinInput
        if (!UserPreferencesRepository.isValidPinFormat(pin)) {
            _lockUi.update {
                it.copy(error = app.getString(R.string.msg_pin_format))
            }
            return
        }
        if (_lockUi.value.isChecking) return

        viewModelScope.launch {
            _lockUi.update { it.copy(isChecking = true, error = null) }
            val ok = preferences.verifyPin(pin)
            if (ok) {
                _sessionUnlocked.value = true
                _lockUi.value = LockUiState()
            } else {
                _lockUi.update {
                    it.copy(
                        isChecking = false,
                        pinInput = "",
                        error = app.getString(R.string.pin_incorrect)
                    )
                }
            }
        }
    }

    fun unlockWithBiometrics() {
        _sessionUnlocked.value = true
        _lockUi.value = LockUiState()
    }

    /** Vuelve a pedir PIN al salir a segundo plano. */
    fun lockSession() {
        if (pinEnabled.value) {
            _sessionUnlocked.value = false
            _lockUi.value = LockUiState()
        }
    }
}
