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
            ManualBlock(
                "Inicio",
                "Consulta el balance consolidado (ingresos − gastos), accesos rápidos y tus últimos movimientos."
            )
            ManualBlock(
                "Movimientos",
                "Elige tipo INGRESO o GASTO, cuenta, categoría y monto. El saldo de la cuenta se actualiza al instante."
            )
            ManualBlock(
                "Proveedores",
                "Crea proveedores o impórtalos. Al pagar, elige la cuenta de origen: se registra un GASTO y baja el saldo."
            )
            ManualBlock(
                "Clientes / CxC",
                "Los clientes con deuda aparecen como PENDIENTE. Al abonar, elige la cuenta destino: se registra un INGRESO y sube el saldo."
            )
            ManualBlock(
                "Contactos",
                "Con permiso de agenda, puedes importar contactos locales y los sincronizados con Google en el teléfono."
            )
            ManualBlock(
                "Dinero preciso",
                "Los montos se guardan en centavos (enteros). Evitamos errores típicos de decimales flotantes."
            )
            ManualBlock(
                "Privacidad",
                "PayControl es offline-first: tu información financiera no se envía a servidores externos."
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
