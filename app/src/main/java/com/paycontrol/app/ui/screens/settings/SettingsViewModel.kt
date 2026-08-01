package com.paycontrol.app.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paycontrol.app.R
import com.paycontrol.app.data.preferences.UserPreferencesRepository
import com.paycontrol.app.domain.util.AppLog
import com.paycontrol.app.reminders.DebtReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val nameDraft: String = "",
    val savedHint: String? = null,
    val savedHintIsError: Boolean = false,
    val permissionHint: String? = null,
    /** Evita pisar lo que el usuario escribe mientras edita. */
    val nameDirty: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    val displayName: StateFlow<String> = preferences.displayName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val pinEnabled: StateFlow<Boolean> = preferences.pinEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val debtRemindersEnabled: StateFlow<Boolean> = preferences.debtRemindersEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _ui = MutableStateFlow(SettingsUiState())
    val ui: StateFlow<SettingsUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.displayName.collect { name ->
                _ui.update { state ->
                    if (state.nameDirty) state
                    else state.copy(nameDraft = name)
                }
            }
        }
    }

    fun onNameDraftChange(value: String) {
        _ui.update {
            it.copy(
                nameDraft = value.take(40),
                nameDirty = true,
                savedHint = null,
                savedHintIsError = false
            )
        }
    }

    fun saveDisplayName() {
        val trimmed = _ui.value.nameDraft.trim()
        if (trimmed.length < 2) {
            _ui.update {
                it.copy(
                    savedHint = appContext.getString(R.string.settings_name_too_short),
                    savedHintIsError = true
                )
            }
            return
        }
        viewModelScope.launch {
            runCatching { preferences.updateDisplayName(trimmed) }
                .onSuccess {
                    _ui.update {
                        it.copy(
                            nameDraft = trimmed,
                            nameDirty = false,
                            savedHint = appContext.getString(R.string.settings_name_updated),
                            savedHintIsError = false
                        )
                    }
                }
                .onFailure { error ->
                    AppLog.e(TAG, "Error al guardar nombre de perfil", error)
                    _ui.update {
                        it.copy(
                            savedHint = appContext.getString(R.string.msg_settings_name_save_failed),
                            savedHintIsError = true
                        )
                    }
                }
        }
    }

    fun clearPermissionHint() {
        _ui.update { it.copy(permissionHint = null) }
    }

    fun setPermissionHint(message: String?) {
        _ui.update { it.copy(permissionHint = message) }
    }

    fun setDebtRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                preferences.setDebtRemindersEnabled(enabled)
                if (enabled) {
                    DebtReminderScheduler.schedule(appContext)
                } else {
                    DebtReminderScheduler.cancel(appContext)
                }
            }.onFailure { error ->
                AppLog.e(TAG, "Error al cambiar recordatorios de cobranza", error)
            }
        }
    }

    companion object {
        private const val TAG = "SettingsVM"
    }
}
