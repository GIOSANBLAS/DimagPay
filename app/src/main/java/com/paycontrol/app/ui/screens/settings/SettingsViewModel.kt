package com.paycontrol.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paycontrol.app.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    val displayName: StateFlow<String> = preferences.displayName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val pinEnabled: StateFlow<Boolean> = preferences.pinEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * @return false if the name is too short; true if a save was scheduled.
     */
    fun saveDisplayName(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.length < 2) return false
        viewModelScope.launch {
            runCatching { preferences.updateDisplayName(trimmed) }
        }
        return true
    }
}
