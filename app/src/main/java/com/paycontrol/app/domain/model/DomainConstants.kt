package com.paycontrol.app.domain.model

import android.content.res.Resources
import com.paycontrol.app.R

/**
 * Tipos de cuenta. Los valores literales se persisten en Room;
 * deben coincidir con [R.array.account_types].
 */
object AccountType {
    const val CASH = "Efectivo"
    const val BANK = "Banco"
    const val CARD = "Tarjeta"
    const val OTHER = "Otro"

    val all = listOf(CASH, BANK, CARD, OTHER)

    fun all(resources: Resources): List<String> =
        resources.getStringArray(R.array.account_types).toList()
}

/**
 * Clasificación de movimiento financiero.
 */
object TransactionType {
    const val INCOME = "INGRESO"
    const val EXPENSE = "GASTO"
    const val TRANSFER = "TRANSFERENCIA"

    val all = listOf(INCOME, EXPENSE)
}

object DefaultCategories {
    /** Fallback offline / tests — debe coincidir con strings.xml */
    val income = listOf("Ventas", "Servicios", "Cobranza", "Intereses", "Otros ingresos")
    val expense = listOf("Compras", "Proveedores", "Nómina", "Renta", "Servicios", "Impuestos", "Otros gastos")

    const val CLIENT_PAYMENT = "Cobranza"
    const val SUPPLIER_PAYMENT = "Proveedores"

    fun income(resources: Resources): List<String> =
        resources.getStringArray(R.array.income_categories).toList()

    fun expense(resources: Resources): List<String> =
        resources.getStringArray(R.array.expense_categories).toList()
}
