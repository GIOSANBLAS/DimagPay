package com.paycontrol.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paycontrol.app.data.preferences.UserPreferencesRepository
import com.paycontrol.app.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WelcomeUiState(
    val name: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

class OnboardingViewModel(
    private val preferences: UserPreferencesRepository,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val optimisticOnboardingDone = MutableStateFlow(false)
    private val optimisticGuideSeen = MutableStateFlow(false)

    val onboardingDone: StateFlow<Boolean?> = combine(
        preferences.onboardingDone,
        optimisticOnboardingDone
    ) { stored, optimistic -> stored || optimistic }
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val guideSeen: StateFlow<Boolean> = combine(
        preferences.guideSeen,
        optimisticGuideSeen
    ) { stored, optimistic -> stored || optimistic }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val displayName: StateFlow<String> = preferences.displayName
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val _welcome = MutableStateFlow(WelcomeUiState())
    val welcome: StateFlow<WelcomeUiState> = _welcome.asStateFlow()

    fun onNameChange(value: String) {
        _welcome.update { it.copy(name = value.take(40), error = null) }
    }

    fun completeWelcome() {
        val name = _welcome.value.name.trim()
        if (name.length < 2) {
            _welcome.update { it.copy(error = "Escribe cómo quieres que te llamemos") }
            return
        }
        if (_welcome.value.isSaving) return

        viewModelScope.launch {
            _welcome.update { it.copy(isSaving = true, error = null) }
            runCatching {
                preferences.completeOnboarding(name)
                optimisticOnboardingDone.value = true
                runCatching { financeRepository.ensureDefaultAccount() }
            }.onFailure { e ->
                if (!optimisticOnboardingDone.value) {
                    _welcome.update {
                        it.copy(
                            isSaving = false,
                            error = "No se pudo guardar tu nombre. Intenta de nuevo."
                        )
                    }
                    return@launch
                }
            }
            _welcome.update { it.copy(isSaving = false) }
        }
    }

    fun markGuideSeen() {
        optimisticGuideSeen.value = true
        viewModelScope.launch {
            runCatching { preferences.markGuideSeen() }
                .onFailure {
                    // Keep optimistic true so the user is not stuck on the guide.
                }
        }
    }
}
