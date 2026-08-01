package com.paycontrol.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.paycontrol.app.data.security.PinHasher
import com.paycontrol.app.data.security.SecureStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Preferencias cifradas (EncryptedSharedPreferences + Android Keystore).
 * El PIN se guarda solo como hash SHA-256 + sal; nunca en texto plano.
 */
class UserPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = SecureStore.encryptedPrefs(context.applicationContext)

    private val _displayName = MutableStateFlow(prefs.getString(Keys.DISPLAY_NAME, "").orEmpty())
    private val _onboardingDone = MutableStateFlow(prefs.getBoolean(Keys.ONBOARDING_DONE, false))
    private val _guideSeen = MutableStateFlow(prefs.getBoolean(Keys.GUIDE_SEEN, false))
    private val _pinEnabled = MutableStateFlow(prefs.getBoolean(Keys.PIN_ENABLED, false))
    private val _debtRemindersEnabled =
        MutableStateFlow(prefs.getBoolean(Keys.DEBT_REMINDERS_ENABLED, false))

    val displayName: StateFlow<String> = _displayName.asStateFlow()
    val onboardingDone: StateFlow<Boolean> = _onboardingDone.asStateFlow()
    val guideSeen: StateFlow<Boolean> = _guideSeen.asStateFlow()
    val pinEnabled: StateFlow<Boolean> = _pinEnabled.asStateFlow()
    val debtRemindersEnabled: StateFlow<Boolean> = _debtRemindersEnabled.asStateFlow()

    private object Keys {
        const val DISPLAY_NAME = "display_name"
        const val ONBOARDING_DONE = "onboarding_done"
        const val GUIDE_SEEN = "guide_seen"
        const val PIN_ENABLED = "pin_enabled"
        const val PIN_HASH = "pin_hash"
        const val PIN_SALT = "pin_salt"
        const val DEBT_REMINDERS_ENABLED = "debt_reminders_enabled"
    }

    suspend fun completeOnboarding(displayName: String) = withContext(Dispatchers.IO) {
        val name = displayName.trim()
        prefs.edit()
            .putString(Keys.DISPLAY_NAME, name)
            .putBoolean(Keys.ONBOARDING_DONE, true)
            .apply()
        _displayName.value = name
        _onboardingDone.value = true
    }

    suspend fun updateDisplayName(displayName: String) = withContext(Dispatchers.IO) {
        val name = displayName.trim()
        prefs.edit().putString(Keys.DISPLAY_NAME, name).apply()
        _displayName.value = name
    }

    suspend fun markGuideSeen() = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(Keys.GUIDE_SEEN, true).apply()
        _guideSeen.value = true
    }

    suspend fun setDebtRemindersEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(Keys.DEBT_REMINDERS_ENABLED, enabled).apply()
        _debtRemindersEnabled.value = enabled
    }

    suspend fun verifyPin(pin: String): Boolean = withContext(Dispatchers.IO) {
        if (!prefs.getBoolean(Keys.PIN_ENABLED, false)) return@withContext false
        val hash = prefs.getString(Keys.PIN_HASH, null) ?: return@withContext false
        val salt = prefs.getString(Keys.PIN_SALT, null) ?: return@withContext false
        PinHasher.verify(pin, salt, hash)
    }

    /**
     * Activa o reemplaza el PIN. Persiste solo hash + sal.
     */
    suspend fun setPin(pin: String) = withContext(Dispatchers.IO) {
        require(isValidPinFormat(pin)) { "PIN inválido" }
        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hash(pin, salt)
        prefs.edit()
            .putBoolean(Keys.PIN_ENABLED, true)
            .putString(Keys.PIN_HASH, hash)
            .putString(Keys.PIN_SALT, salt)
            .apply()
        _pinEnabled.value = true
    }

    suspend fun changePin(currentPin: String, newPin: String): Boolean = withContext(Dispatchers.IO) {
        if (!verifyPinBlocking(currentPin)) return@withContext false
        require(isValidPinFormat(newPin)) { "PIN inválido" }
        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hash(newPin, salt)
        prefs.edit()
            .putBoolean(Keys.PIN_ENABLED, true)
            .putString(Keys.PIN_HASH, hash)
            .putString(Keys.PIN_SALT, salt)
            .apply()
        _pinEnabled.value = true
        true
    }

    suspend fun disablePin(currentPin: String): Boolean = withContext(Dispatchers.IO) {
        if (!verifyPinBlocking(currentPin)) return@withContext false
        prefs.edit()
            .putBoolean(Keys.PIN_ENABLED, false)
            .remove(Keys.PIN_HASH)
            .remove(Keys.PIN_SALT)
            .apply()
        _pinEnabled.value = false
        true
    }

    private fun verifyPinBlocking(pin: String): Boolean {
        if (!prefs.getBoolean(Keys.PIN_ENABLED, false)) return false
        val hash = prefs.getString(Keys.PIN_HASH, null) ?: return false
        val salt = prefs.getString(Keys.PIN_SALT, null) ?: return false
        return PinHasher.verify(pin, salt, hash)
    }

    companion object {
        const val PIN_MIN_LENGTH = 4
        const val PIN_MAX_LENGTH = 8

        fun isValidPinFormat(pin: String): Boolean =
            pin.length in PIN_MIN_LENGTH..PIN_MAX_LENGTH && pin.all { it.isDigit() }
    }
}
