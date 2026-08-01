package com.paycontrol.app.domain.util

import android.content.res.Resources
import com.paycontrol.app.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapea excepciones de dominio/persistencia a mensajes seguros para la UI.
 * Evita filtrar detalles internos de SQLite/SQLCipher.
 */
@Singleton
class UiErrorMapper @Inject constructor(
    private val resources: Resources
) {

    fun map(error: Throwable, fallback: String): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("UNIQUE", ignoreCase = true) ->
                resources.getString(R.string.error_unique_name)
            message.contains("Cannot access database", ignoreCase = true) ||
                message.contains("SQLCipher", ignoreCase = true) ||
                message.contains("SQLite", ignoreCase = true) ->
                resources.getString(R.string.error_database_access)
            // Mensajes de dominio ya localizados (DomainStrings / require / error).
            error is IllegalArgumentException || error is IllegalStateException ->
                message.ifBlank { fallback }
            else -> fallback
        }
    }
}
