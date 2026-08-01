package com.paycontrol.app.domain.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Fechas de negocio se almacenan como epoch millis UTC ([Instant]).
 * La UI siempre formatea en [ZoneId.systemDefault()].
 */
object DateTimeUtils {
    private val esMx: Locale =
        Locale.Builder().setLanguage("es").setRegion("MX").build()

    private val dayMonthYear: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy", esMx)

    private val dayMonthYearNumeric: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy", esMx)

    fun nowEpochMillis(): Long = Instant.now().toEpochMilli()

    fun toInstant(epochMillis: Long): Instant = Instant.ofEpochMilli(epochMillis)

    fun startOfLocalDayMillis(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val localDate = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        return localDate.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun endOfLocalDayMillis(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val localDate = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        return localDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1L
    }

    fun startOfLocalDayMillis(localDate: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        localDate.atStartOfDay(zone).toInstant().toEpochMilli()

    fun formatDisplay(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).format(dayMonthYear)

    fun formatNumeric(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).format(dayMonthYearNumeric)
}
