package com.paycontrol.app.ui.navigation

sealed class AppDestination(
    val route: String,
    val label: String,
    /** Etiqueta corta para la barra inferior (evita saltos tipo «Proveedore / s»). */
    val navLabel: String = label
) {
    data object Welcome : AppDestination("welcome", "Bienvenida")
    data object Guide : AppDestination("guide", "Guía")
    data object Dashboard : AppDestination("dashboard", "Inicio")
    data object Transactions : AppDestination("transactions", "Movimientos", navLabel = "Movs")
    data object Suppliers : AppDestination("suppliers", "Proveedores", navLabel = "Prov.")
    data object Clients : AppDestination("clients", "Clientes")
    data object Accounts : AppDestination("accounts", "Cuentas")
    data object Settings : AppDestination("settings", "Ajustes")
    data object PinLock : AppDestination("pin_lock", "Bloqueo PIN")
    data object Reports : AppDestination("reports", "Reportes")
    data object Backup : AppDestination("backup", "Respaldo")
    data object Manual : AppDestination("manual", "Manual")
    data object About : AppDestination("about", "Acerca de")
    data object Changelog : AppDestination("changelog", "Cambios")
    data object Team : AppDestination("team", "Equipo")
    data object Licenses : AppDestination("licenses", "Licencias")
    data object Privacy : AppDestination("privacy", "Privacidad")

    companion object {
        val bottomBarItems = listOf(Dashboard, Transactions, Suppliers, Clients, Settings)
    }
}
