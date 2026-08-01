package com.paycontrol.app.domain.model

object AppInfo {
    const val APP_NAME = "DimagPay"
    const val VERSION_NAME = "1.3.0-Atlas"
    const val VERSION_CODE = 4
    const val BUILD_CODENAME = "Atlas"
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
            version = "1.3.0-Atlas",
            date = "Agosto 2026",
            highlights = listOf(
                "Build nombrada Atlas (versionName 1.3.0-Atlas)",
                "Estado de UI centralizado en ViewModels",
                "Logging Timber + AppLog (sin secretos)",
                "Backup con confirmación de contraseña, fortaleza e inventario",
                "Paging 3 en movimientos, clientes y proveedores",
                "Inyección Hilt",
                "Fechas UTC + índices compuestos (Room v3)",
                "ARCHITECTURE.md y pruebas de política/cifrado"
            )
        ),
        ChangelogEntry(
            version = "1.2.0",
            date = "Agosto 2026",
            highlights = listOf(
                "Identidad visual Sapphire Atelier (tipografía Fraunces + Figtree)",
                "Nuevo icono de launcher y marca DimagPay",
                "Dashboard y bienvenida rediseñados",
                "Corrección de crash al pedir permiso de notificaciones (Fragment)"
            )
        ),
        ChangelogEntry(
            version = "1.1.0",
            date = "Agosto 2026",
            highlights = listOf(
                "Renombre de producto a DimagPay",
                "Transferencias entre cuentas",
                "Respaldo cifrado con contraseña (AES-GCM)",
                "Recordatorios de cobranza",
                "Gráfica de ingresos vs gastos en reportes",
                "Widget de saldo (respeta PIN)",
                "Integridad: transferencias emparejadas, anti doble-tap, revertir CxC al borrar",
                "Manual de usuario actualizado"
            )
        ),
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
                "Abonos y pagos vinculados al saldo de cuentas",
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
        "AndroidX Jetpack (Compose, Room, Lifecycle, Navigation, WorkManager) — Apache License 2.0",
        "Kotlin Coroutines — Apache License 2.0",
        "AndroidX Security Crypto — Apache License 2.0",
        "SQLCipher for Android — BSD-style / Zetetic",
        "Accompanist Permissions — Apache License 2.0",
        "Material Icons — Apache License 2.0",
        "Fraunces y Figtree — SIL Open Font License 1.1"
    )

    val privacySummary = """
        DimagPay es offline-first. Tus datos financieros se guardan cifrados en este dispositivo
        (SQLCipher + Android Keystore). El nombre de perfil también se almacena cifrado.

        Las copias de seguridad automáticas del sistema y la transferencia de dispositivo están
        deshabilitadas. Puedes exportar un respaldo manual desde Ajustes cuando lo necesites.

        Si autorizas contactos, la app solo busca nombre y teléfono bajo demanda para crear
        clientes o proveedores. No inicia sesión en Google ni envía información a servidores.

        Los recordatorios de cobranza son notificaciones locales en este dispositivo.
    """.trimIndent()
}
