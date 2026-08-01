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
            message.contains("Saldo insuficiente", ignoreCase = true) -> message
            message.contains("supera la deuda", ignoreCase = true) -> message
            message.contains("No se puede eliminar", ignoreCase = true) -> message
            message.contains("No se puede revertir", ignoreCase = true) -> message
            message.contains("origen y destino", ignoreCase = true) -> message
            message.contains("Tipo de transferencia", ignoreCase = true) -> message
            message.contains("no encontrado", ignoreCase = true) ||
                message.contains("no encontrada", ignoreCase = true) -> message
            message.contains("obligatorio", ignoreCase = true) ||
                message.contains("mayor a cero", ignoreCase = true) ||
                message.contains("demasiado largo", ignoreCase = true) ||
                message.contains("inválid", ignoreCase = true) ||
                message.contains("no puede ser", ignoreCase = true) ||
                message.contains("respaldo", ignoreCase = true) ||
                message.contains("esquema", ignoreCase = true) ||
                message.contains("contraseña", ignoreCase = true) ||
                message.contains("dañado", ignoreCase = true) ||
                message.contains("coinciden", ignoreCase = true) ||
                message.contains("símbolo", ignoreCase = true) -> message
            message.contains("Cannot access database", ignoreCase = true) ||
                message.contains("SQLCipher", ignoreCase = true) ||
                message.contains("SQLite", ignoreCase = true) ->
                resources.getString(R.string.error_database_access)
            else -> fallback
        }
    }
}
