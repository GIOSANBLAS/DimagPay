package com.paycontrol.app.ui.screens.manual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paycontrol.app.domain.model.AppInfo
import com.paycontrol.app.ui.components.SoftPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManualScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manual de usuario") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SoftPanel {
                Text(
                    AppInfo.APP_NAME,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Guía ${AppInfo.VERSION_NAME}. Todo funciona offline en tu dispositivo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ManualBlock(
                "Inicio",
                "Consulta el balance consolidado (ingresos − gastos), atajos a movimientos, reportes, proveedores, clientes y cuentas, y tus últimos movimientos."
            )
            ManualBlock(
                "Movimientos",
                "Registra INGRESO o GASTO: elige cuenta, categoría y monto. El saldo de la cuenta se actualiza al instante. Puedes eliminar un movimiento; el saldo se revierte."
            )
            ManualBlock(
                "Cuentas",
                "Crea cuentas (efectivo, banco, tarjeta u otro) con saldo inicial opcional. Edita nombre/tipo o elimina solo si no tienen movimientos. Usa Transferir para mover dinero entre cuentas sin inflar ingresos ni gastos."
            )
            ManualBlock(
                "Proveedores",
                "Crea proveedores o impórtalos desde contactos. Al pagar, elige la cuenta de origen: se registra un GASTO y baja el saldo. Puedes editar o eliminar proveedores."
            )
            ManualBlock(
                "Clientes / CxC",
                "Registra deudas por cobrar. Al abonar, elige la cuenta destino: se registra un INGRESO y sube el saldo. También puedes agregar deuda, editar o eliminar clientes."
            )
            ManualBlock(
                "Reportes",
                "Filtra por periodo (hoy, 7/30 días o todo), cuenta, tipo y categoría. Revisa totales, la gráfica ingresos vs gastos y exporta CSV para compartir."
            )
            ManualBlock(
                "Contactos",
                "Con permiso de agenda, busca e importa contactos locales o sincronizados con Google. Solo se lee nombre y teléfono bajo demanda."
            )
            ManualBlock(
                "Bloqueo PIN",
                "En Ajustes puedes activar un PIN (4–8 dígitos). Opcionalmente desbloquea con biometría. El PIN se guarda cifrado en el dispositivo."
            )
            ManualBlock(
                "Respaldo y restauración",
                "En Ajustes exporta un archivo JSON de respaldo y compártelo (Drive, correo, etc.). Restaurar reemplaza todos los datos actuales: confirma antes de continuar."
            )
            ManualBlock(
                "Recordatorios",
                "Activa recordatorios de cobranza en Ajustes. DimagPay te avisará en el dispositivo si tienes saldos por cobrar. Requiere permiso de notificaciones en Android 13+."
            )
            ManualBlock(
                "Widget",
                "Añade el widget de DimagPay a la pantalla de inicio para ver el saldo consolidado sin abrir la app."
            )
            ManualBlock(
                "Dinero preciso",
                "Los montos se guardan en centavos (enteros). Así se evitan errores típicos de decimales flotantes."
            )
            ManualBlock(
                "Privacidad y seguridad",
                "DimagPay es offline-first: base cifrada (SQLCipher), preferencias cifradas y sin envío a servidores. Las copias automáticas del sistema están desactivadas; usa el respaldo manual."
            )
            ManualBlock(
                "Equipo",
                "Producto: Giosánblas. Arquitectura: Auto."
            )
        }
    }
}

@Composable
private fun ManualBlock(title: String, body: String) {
    SoftPanel {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
