package com.paycontrol.app.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

/**
 * Materiales criptográficos locales (Android Keystore + EncryptedSharedPreferences).
 * La passphrase de Room/SQLCipher nunca se hardcodea en el binario.
 */
object SecureStore {

    private const val PREFS = "paycontrol_secure_store"
    private const val KEY_DB_PASS = "db_passphrase"

    fun encryptedPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS,
        masterKey(context),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun databasePassphrase(context: Context): ByteArray {
        val prefs = encryptedPrefs(context)
        val existing = prefs.getString(KEY_DB_PASS, null)
        if (!existing.isNullOrBlank()) {
            return existing.toByteArray(Charsets.UTF_8)
        }
        val fresh = UUID.randomUUID().toString() + UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DB_PASS, fresh).commit()
        return fresh.toByteArray(Charsets.UTF_8)
    }

    private fun masterKey(context: Context): MasterKey =
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
}
