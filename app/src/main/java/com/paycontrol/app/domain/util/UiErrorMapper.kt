package com.paycontrol.app.domain.util

/**
 * Mapea excepciones de dominio/persistencia a mensajes seguros para la UI.
 * Evita filtrar detalles internos de SQLite/SQLCipher.
 */
object UiErrorMapper {

    fun map(error: Throwable, fallback: String): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("UNIQUE", ignoreCase = true) ->
                "Ya existe un registro con ese nombre"
            message.contains("Saldo insuficiente", ignoreCase = true) -> message
            message.contains("supera la deuda", ignoreCase = true) -> message
            message.contains("No se puede eliminar", ignoreCase = true) -> message
            message.contains("no encontrado", ignoreCase = true) ||
                message.contains("no encontrada", ignoreCase = true) -> message
            message.contains("obligatorio", ignoreCase = true) ||
                message.contains("mayor a cero", ignoreCase = true) ||
                message.contains("demasiado largo", ignoreCase = true) ||
                message.contains("inválid", ignoreCase = true) ||
                message.contains("no puede ser", ignoreCase = true) -> message
            message.contains("Cannot access database", ignoreCase = true) ||
                message.contains("SQLCipher", ignoreCase = true) ||
                message.contains("SQLite", ignoreCase = true) ->
                "No se pudo acceder a los datos. Intenta de nuevo."
            else -> fallback
        }
    }
}
