package com.paycontrol.app.domain.model

import android.content.res.Resources
import com.paycontrol.app.R

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

    /** Changelog de producto (contenido editorial; no UI de formularios). */
    val changelog = listOf(
        ChangelogEntry(
            version = "1.3.0-Atlas",
            date = "Agosto 2026",
            highlights = listOf(
                "Build nombrada Atlas (versionName 1.3.0-Atlas)",
                "Estado de UI centralizado en ViewModels",
                "Logging Timber + AppLog (sin secretos)",
                "Backup con confirmación de contraseña, fortaleza e inventario",
                "Paging 3 en movimientos, clientes, proveedores y reportes",
                "Inyección Hilt",
                "Fechas UTC + índices compuestos (Room v3)",
                "ARCHITECTURE.md y pruebas de repositorio/migraciones"
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

    fun team(resources: Resources): List<TeamMember> = listOf(
        TeamMember("Giosánblas", resources.getString(R.string.formal_team_role_giosanblas)),
        TeamMember("Auto", resources.getString(R.string.formal_team_role_auto))
    )

    fun licenses(resources: Resources): List<String> =
        resources.getStringArray(R.array.formal_licenses).toList()

    fun privacySummary(resources: Resources): String =
        resources.getString(R.string.formal_privacy_summary)
}
