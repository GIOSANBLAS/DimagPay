package com.paycontrol.app.domain.util

/**
 * Política de contraseña de respaldo cifrado.
 * No registra ni retorna la contraseña en claro hacia logs.
 */
object BackupPasswordPolicy {
    const val MIN_LENGTH = 8

    private val hasLetter = Regex("[A-Za-zÁÉÍÓÚáéíóúÑñ]")
    private val hasDigit = Regex("\\d")
    private val hasSymbol = Regex("[^A-Za-zÁÉÍÓÚáéíóúÑñ0-9\\s]")

    enum class Strength { WEAK, MEDIUM, STRONG }

    fun validate(password: String): String? {
        if (password.length < MIN_LENGTH) {
            return "Mínimo $MIN_LENGTH caracteres"
        }
        if (!hasLetter.containsMatchIn(password)) {
            return "Debe incluir al menos una letra"
        }
        if (!hasDigit.containsMatchIn(password)) {
            return "Debe incluir al menos un número"
        }
        if (!hasSymbol.containsMatchIn(password)) {
            return "Debe incluir al menos un símbolo (ej. !@#\$%)"
        }
        return null
    }

    fun strength(password: String): Strength {
        if (password.isEmpty()) return Strength.WEAK
        var score = 0
        if (password.length >= MIN_LENGTH) score++
        if (password.length >= 12) score++
        if (hasLetter.containsMatchIn(password)) score++
        if (hasDigit.containsMatchIn(password)) score++
        if (hasSymbol.containsMatchIn(password)) score++
        if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
        return when {
            score >= 5 -> Strength.STRONG
            score >= 3 -> Strength.MEDIUM
            else -> Strength.WEAK
        }
    }

    fun strengthLabel(strength: Strength): String = when (strength) {
        Strength.WEAK -> "Débil"
        Strength.MEDIUM -> "Media"
        Strength.STRONG -> "Fuerte"
    }
}
