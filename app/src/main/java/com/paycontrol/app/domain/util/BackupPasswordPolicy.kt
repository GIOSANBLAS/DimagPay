package com.paycontrol.app.domain.util

import android.content.res.Resources
import com.paycontrol.app.R

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

    enum class Issue {
        TOO_SHORT,
        NO_LETTER,
        NO_DIGIT,
        NO_SYMBOL
    }

    fun validate(password: String): Issue? {
        if (password.length < MIN_LENGTH) return Issue.TOO_SHORT
        if (!hasLetter.containsMatchIn(password)) return Issue.NO_LETTER
        if (!hasDigit.containsMatchIn(password)) return Issue.NO_DIGIT
        if (!hasSymbol.containsMatchIn(password)) return Issue.NO_SYMBOL
        return null
    }

    fun issueMessage(issue: Issue, resources: Resources): String = when (issue) {
        Issue.TOO_SHORT -> resources.getString(R.string.backup_policy_min_length, MIN_LENGTH)
        Issue.NO_LETTER -> resources.getString(R.string.backup_policy_letter)
        Issue.NO_DIGIT -> resources.getString(R.string.backup_policy_digit)
        Issue.NO_SYMBOL -> resources.getString(R.string.backup_policy_symbol)
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

    fun strengthLabel(strength: Strength, resources: Resources): String = when (strength) {
        Strength.WEAK -> resources.getString(R.string.backup_strength_weak)
        Strength.MEDIUM -> resources.getString(R.string.backup_strength_medium)
        Strength.STRONG -> resources.getString(R.string.backup_strength_strong)
    }
}
