package com.paycontrol.app.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paycontrol.app.ui.components.IconBadge
import com.paycontrol.app.ui.components.SoftPanel

@Composable
fun GuideScreen(
    displayName: String,
    onContinue: () -> Unit,
    onOpenManual: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (displayName.isBlank()) "Tu guía rápida" else "Hola, $displayName",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Te acompañamos en los primeros pasos. Todo queda en tu teléfono.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        GuideCard(
            icon = Icons.Outlined.AccountBalanceWallet,
            title = "1. Revisa tu inicio",
            body = "El dashboard muestra ingresos, gastos y el balance consolidado."
        )
        GuideCard(
            icon = Icons.Outlined.SwapHoriz,
            title = "2. Registra movimientos",
            body = "Anota entradas y salidas asociadas a una cuenta en segundos."
        )
        GuideCard(
            icon = Icons.Outlined.Groups,
            title = "3. Clientes y proveedores",
            body = "Controla cuentas por cobrar y pagos a proveedores."
        )
        GuideCard(
            icon = Icons.Outlined.Contacts,
            title = "4. Importa contactos",
            body = "Usa tu agenda local o de Google sincronizada para crear fichas rápido."
        )
        GuideCard(
            icon = Icons.AutoMirrored.Outlined.MenuBook,
            title = "5. Manual y ajustes",
            body = "Consulta el manual, historial de cambios, equipo y licencias cuando quieras."
        )

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entendido, ir al inicio")
        }
        TextButton(
            onClick = onOpenManual,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Abrir manual de usuario")
        }
    }
}

@Composable
private fun GuideCard(
    icon: ImageVector,
    title: String,
    body: String
) {
    SoftPanel {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(icon = icon, contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
