package com.paycontrol.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paycontrol.app.domain.model.AppInfo
import com.paycontrol.app.ui.components.SectionTitle
import com.paycontrol.app.ui.components.SettingsRow
import com.paycontrol.app.ui.components.SoftPanel
import com.paycontrol.app.ui.navigation.AppDestination

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigate: (AppDestination) -> Unit
) {
    val displayName by viewModel.displayName.collectAsStateWithLifecycle()
    val pinEnabled by viewModel.pinEnabled.collectAsStateWithLifecycle()
    var nameDraft by remember(displayName) { mutableStateOf(displayName) }
    var savedHint by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(displayName) {
        nameDraft = displayName
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle(
            title = "Ajustes",
            subtitle = "Preferencias, ayuda y información formal de PayControl."
        )

        SoftPanel {
            Text("Cómo te llamamos", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = nameDraft,
                onValueChange = { nameDraft = it.take(40) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                label = { Text("Nombre o apodo") }
            )
            TextButton(
                onClick = {
                    savedHint = if (viewModel.saveDisplayName(nameDraft)) {
                        "Nombre actualizado"
                    } else {
                        "Escribe al menos 2 caracteres"
                    }
                }
            ) {
                Text("Guardar")
            }
            savedHint?.let {
                Text(
                    it,
                    color = if (it.startsWith("Escribe")) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        }

        SettingsRow(
            icon = Icons.Outlined.AccountBalanceWallet,
            title = "Cuentas",
            subtitle = "Crear y administrar efectivo, banco y más",
            onClick = { onNavigate(AppDestination.Accounts) }
        )
        SettingsRow(
            icon = Icons.Outlined.Assessment,
            title = "Reportes",
            subtitle = "Filtros, totales e exportación CSV",
            onClick = { onNavigate(AppDestination.Reports) }
        )
        SettingsRow(
            icon = Icons.Outlined.Lock,
            title = "Bloqueo por PIN",
            subtitle = if (pinEnabled) {
                "Activado · Cambiar o desactivar"
            } else {
                "Protege la app con un PIN (opcional biometría)"
            },
            onClick = { onNavigate(AppDestination.PinLock) }
        )
        SettingsRow(
            icon = Icons.AutoMirrored.Outlined.MenuBook,
            title = "Manual de usuario",
            subtitle = "Cómo usar cada módulo paso a paso",
            onClick = { onNavigate(AppDestination.Manual) }
        )
        SettingsRow(
            icon = Icons.Outlined.Info,
            title = "Acerca de",
            subtitle = "Versión ${AppInfo.VERSION_NAME} · ${AppInfo.COMPANY}",
            onClick = { onNavigate(AppDestination.About) }
        )
        SettingsRow(
            icon = Icons.Outlined.HistoryEdu,
            title = "Historial de cambios",
            subtitle = "Novedades de cada versión",
            onClick = { onNavigate(AppDestination.Changelog) }
        )
        SettingsRow(
            icon = Icons.Outlined.Groups,
            title = "Nuestro equipo",
            subtitle = "Quiénes construyen PayControl",
            onClick = { onNavigate(AppDestination.Team) }
        )
        SettingsRow(
            icon = Icons.Outlined.Verified,
            title = "Licencias de código abierto",
            subtitle = "Atribuciones legales de dependencias",
            onClick = { onNavigate(AppDestination.Licenses) }
        )
        SettingsRow(
            icon = Icons.Outlined.Policy,
            title = "Privacidad",
            subtitle = "Cómo tratamos tus datos offline",
            onClick = { onNavigate(AppDestination.Privacy) }
        )
    }
}
