package com.paycontrol.app.domain.model

object AppInfo {
    const val VERSION_NAME = "1.0.0"
    const val VERSION_CODE = 1
    const val COMPANY = "Giosánblas"
}

data class ChangelogEntry(
    val version: String,
    val date: String,
    val highlights: List<String>
)

data class TeamMember(
    val name: String,
    val role: String
)

object FormalContent {

    val changelog = listOf(
        ChangelogEntry(
            version = "1.0.0",
            date = "Agosto 2026",
            highlights = listOf(
                "Lanzamiento inicial offline-first",
                "Dashboard consolidado de ingresos y gastos",
                "Módulos de movimientos, proveedores y clientes (CxC)",
                "Importación de contactos locales y de Google",
                "Onboarding personalizado y manual de usuario",
                "Montos almacenados en centavos para precisión monetaria",
                "Abonos y pagos vinculados al saldo de cuentas (movimientos automáticos)",
                "Gestión de cuentas, reportes con filtros y exportación CSV",
                "Edición/eliminación de registros y bloqueo por PIN",
                "Cifrado SQLCipher + preferencias seguras"
            )
        )
    )

    val team = listOf(
        TeamMember("Giosánblas", "Producto, visión y dirección del proyecto"),
        TeamMember("Auto", "Arquitectura Android · Kotlin · Compose · Room")
    )

    val licenses = listOf(
        "Kotlin — Apache License 2.0",
        "AndroidX Jetpack (Compose, Room, Lifecycle, Navigation) — Apache License 2.0",
        "Kotlin Coroutines — Apache License 2.0",
        "AndroidX Security Crypto — Apache License 2.0",
        "SQLCipher for Android — BSD-style / Zetetic",
        "Accompanist Permissions — Apache License 2.0",
        "Material Icons — Apache License 2.0"
    )

    val privacySummary = """
        PayControl es offline-first. Tus datos financieros se guardan cifrados en este dispositivo
        (SQLCipher + Android Keystore). El nombre de perfil también se almacena cifrado.

        Las copias de seguridad automáticas y la transferencia de dispositivo están deshabilitadas
        para este tipo de datos.

        Si autorizas contactos, la app solo busca nombre y teléfono bajo demanda para crear
        clientes o proveedores. No inicia sesión en Google ni envía información a servidores.
    """.trimIndent()
}
