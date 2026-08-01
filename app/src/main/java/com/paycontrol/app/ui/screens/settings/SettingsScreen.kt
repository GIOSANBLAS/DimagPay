package com.paycontrol.app.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paycontrol.app.R
import com.paycontrol.app.domain.model.AppInfo
import com.paycontrol.app.ui.components.SectionTitle
import com.paycontrol.app.ui.components.SettingsRow
import com.paycontrol.app.ui.components.SettingsToggleRow
import com.paycontrol.app.ui.components.SoftPanel
import com.paycontrol.app.ui.navigation.AppDestination

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigate: (AppDestination) -> Unit
) {
    val context = LocalContext.current
    val pinEnabled by viewModel.pinEnabled.collectAsStateWithLifecycle()
    val debtRemindersEnabled by viewModel.debtRemindersEnabled.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val permissionDeniedMessage = stringResource(R.string.settings_debt_reminders_permission_denied)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.clearPermissionHint()
            viewModel.setDebtRemindersEnabled(true)
        } else {
            viewModel.setPermissionHint(permissionDeniedMessage)
        }
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
            subtitle = "Preferencias, ayuda y información formal de DimagPay."
        )

        SoftPanel {
            Text("Cómo te llamamos", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = ui.nameDraft,
                onValueChange = viewModel::onNameDraftChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                label = { Text("Nombre o apodo") }
            )
            TextButton(onClick = viewModel::saveDisplayName) {
                Text("Guardar")
            }
            ui.savedHint?.let { hint ->
                Text(
                    hint,
                    color = if (ui.savedHintIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        }

        SettingsToggleRow(
            icon = Icons.Outlined.Notifications,
            title = stringResource(R.string.settings_debt_reminders_title),
            subtitle = if (debtRemindersEnabled) {
                stringResource(R.string.settings_debt_reminders_subtitle_on)
            } else {
                stringResource(R.string.settings_debt_reminders_subtitle_off)
            },
            checked = debtRemindersEnabled,
            onCheckedChange = { enabled ->
                viewModel.clearPermissionHint()
                if (!enabled) {
                    viewModel.setDebtRemindersEnabled(false)
                    return@SettingsToggleRow
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        viewModel.setDebtRemindersEnabled(true)
                    } else {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    viewModel.setDebtRemindersEnabled(true)
                }
            }
        )
        ui.permissionHint?.let { hint ->
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
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
            icon = Icons.Outlined.CloudUpload,
            title = "Respaldo",
            subtitle = "Exportar y restaurar JSON de DimagPay",
            onClick = { onNavigate(AppDestination.Backup) }
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
            subtitle = "Quiénes construyen DimagPay",
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
