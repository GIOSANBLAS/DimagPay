package com.paycontrol.app.domain.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Montos en centavos (Long). Aritmética exacta; sin Double en persistencia.
 */
object Money {

    private const val SCALE = 2
    private val HUNDRED = BigDecimal(100)
    private const val MAX_ABS_CENTS = 9_999_999_999_99L // ~99,999,999,999.99

    fun parseToCents(input: String): Long? {
        val normalized = input.trim().replace(',', '.')
        if (normalized.isEmpty()) return null
        if (!normalized.matches(Regex("""^-?\d+(\.\d{0,2})?$"""))) return null
        return runCatching {
            val cents = BigDecimal(normalized)
                .setScale(SCALE, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .longValueExact()
            require(kotlin.math.abs(cents) <= MAX_ABS_CENTS)
            cents
        }.getOrNull()
    }

    fun centsToDecimal(cents: Long): BigDecimal =
        BigDecimal(cents).divide(HUNDRED, SCALE, RoundingMode.HALF_UP)

    fun format(cents: Long, locale: Locale = Locale.Builder().setLanguage("es").setRegion("MX").build()): String {
        val formatter = NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance("MXN")
            minimumFractionDigits = SCALE
            maximumFractionDigits = SCALE
        }
        return formatter.format(centsToDecimal(cents))
    }

    fun add(a: Long, b: Long): Long = Math.addExact(a, b)

    fun subtract(a: Long, b: Long): Long = Math.subtractExact(a, b)

    fun negate(cents: Long): Long = Math.negateExact(cents)
}
