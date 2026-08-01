package com.paycontrol.app.domain.model

/**
 * Tipos de cuenta bancaria / efectivo.
 * Persistidos como String en Room para flexibilidad offline.
 */
object AccountType {
    const val CASH = "Efectivo"
    const val BANK = "Banco"
    const val CARD = "Tarjeta"
    const val OTHER = "Otro"

    val all = listOf(CASH, BANK, CARD, OTHER)
}

/**
 * Clasificación de movimiento financiero.
 */
object TransactionType {
    const val INCOME = "INGRESO"
    const val EXPENSE = "GASTO"

    val all = listOf(INCOME, EXPENSE)
}

object DefaultCategories {
    val income = listOf("Ventas", "Servicios", "Cobranza", "Intereses", "Otros ingresos")
    val expense = listOf("Compras", "Proveedores", "Nómina", "Renta", "Servicios", "Impuestos", "Otros gastos")

    const val CLIENT_PAYMENT = "Cobranza"
    const val SUPPLIER_PAYMENT = "Proveedores"
}
