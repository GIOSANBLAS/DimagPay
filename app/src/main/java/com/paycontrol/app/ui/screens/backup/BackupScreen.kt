package com.paycontrol.app.ui.screens.backup

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paycontrol.app.data.backup.BackupManager
import com.paycontrol.app.domain.util.BackupPasswordPolicy
import com.paycontrol.app.security.AppLockGate
import com.paycontrol.app.ui.components.SectionTitle
import com.paycontrol.app.ui.components.SettingsRow
import com.paycontrol.app.ui.components.SoftPanel
import com.paycontrol.app.ui.components.StatusMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val openDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        viewModel.onRestoreFilePicked(uri)
    }

    LaunchedEffect(Unit) {
        viewModel.shareEvents.collect { intent ->
            AppLockGate.suppressFor()
            context.startActivity(
                Intent.createChooser(intent, "Compartir respaldo DimagPay")
            )
        }
    }

    if (state.showExportPassword) {
        BackupPasswordDialog(
            title = "Cifrar respaldo",
            body = "El archivo se cifrará con tu contraseña. Sin ella no podrás restaurar " +
                "los datos. Guárdala en un lugar seguro.",
            inventory = state.inventory,
            inventoryIsCurrentData = true,
            requireConfirm = true,
            confirmLabel = "Exportar",
            confirmEnabled = !state.isExporting,
            onDismiss = viewModel::dismissExportPassword,
            onConfirm = { password, confirm ->
                viewModel.exportBackup(password, confirm)
            }
        )
    }

    if (state.showRestoreConfirm) {
        BackupPasswordDialog(
            title = "Restaurar respaldo",
            body = "Esta acción reemplazará todas las cuentas, movimientos, clientes y " +
                "proveedores actuales con los del archivo. No se puede deshacer.\n\n" +
                "El archivo está cifrado: introduce la contraseña con la que se exportó.",
            inventory = state.inventory,
            inventoryIsCurrentData = true,
            inventoryWarning = "Los datos actuales se reemplazarán por los del archivo.",
            requireConfirm = false,
            confirmLabel = "Restaurar",
            confirmEnabled = !state.isRestoring,
            confirmIsDestructive = true,
            onDismiss = viewModel::dismissRestoreConfirm,
            onConfirm = { password, _ ->
                viewModel.confirmRestore(password)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Respaldo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Volver"
                        )
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
            SectionTitle(
                title = "Respaldo y restauración",
                subtitle = "Copia cifrada de DimagPay. La restauración reemplaza todos los datos."
            )

            SoftPanel {
                Text(
                    "Exporta tus datos a un archivo JSON cifrado con contraseña y " +
                        "compártelo por correo, Drive u otra app. Para restaurar, elige " +
                        "un respaldo DimagPay e introduce la misma contraseña.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsRow(
                icon = Icons.Outlined.CloudUpload,
                title = "Respaldo",
                subtitle = if (state.isExporting) {
                    "Generando archivo cifrado…"
                } else {
                    "Exportar y compartir JSON cifrado de DimagPay"
                },
                onClick = {
                    if (!state.isExporting && !state.isRestoring) {
                        viewModel.requestExport()
                    }
                }
            )
            SettingsRow(
                icon = Icons.Outlined.Restore,
                title = "Restaurar",
                subtitle = if (state.isRestoring) {
                    "Restaurando datos…"
                } else {
                    "Elegir archivo cifrado y reemplazar datos"
                },
                onClick = {
                    if (!state.isExporting && !state.isRestoring) {
                        AppLockGate.suppressFor()
                        openDocument.launch(arrayOf("application/json", "text/*", "*/*"))
                    }
                }
            )

            StatusMessage(state.errorMessage, state.successMessage)
        }
    }
}

@Composable
private fun BackupPasswordDialog(
    title: String,
    body: String,
    inventory: BackupManager.Inventory?,
    inventoryIsCurrentData: Boolean,
    inventoryWarning: String? = null,
    requireConfirm: Boolean,
    confirmLabel: String,
    confirmEnabled: Boolean,
    confirmIsDestructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (password: String, confirmPassword: String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val validationError = BackupPasswordPolicy.validate(password)
    val strength = BackupPasswordPolicy.strength(password)
    val passwordsMatch = !requireConfirm || password == confirmPassword
    val canSubmit = confirmEnabled &&
        validationError == null &&
        passwordsMatch &&
        password.isNotEmpty()

    AlertDialog(
        onDismissRequest = {
            if (confirmEnabled) onDismiss()
        },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(body)
                InventorySummary(
                    inventory = inventory,
                    isCurrentData = inventoryIsCurrentData,
                    warning = inventoryWarning
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled = confirmEnabled,
                    isError = password.isNotEmpty() && validationError != null,
                    supportingText = {
                        val tip = validationError
                            ?: "Letras, números y un símbolo. Mín. ${BackupPasswordPolicy.MIN_LENGTH}."
                        Text(tip)
                    }
                )
                if (password.isNotEmpty()) {
                    StrengthMeter(strength)
                }
                if (requireConfirm) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Confirmar contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        enabled = confirmEnabled,
                        isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                        supportingText = {
                            if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                                Text("Las contraseñas no coinciden")
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password, confirmPassword) },
                enabled = canSubmit
            ) {
                Text(
                    confirmLabel,
                    color = if (confirmIsDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = confirmEnabled
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun InventorySummary(
    inventory: BackupManager.Inventory?,
    isCurrentData: Boolean,
    warning: String?
) {
    SoftPanel {
        Text(
            if (isCurrentData) "Datos actuales en DimagPay" else "Contenido del respaldo",
            style = MaterialTheme.typography.titleSmall
        )
        if (inventory == null) {
            Text(
                "No se pudo leer el resumen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text("• Cuentas: ${inventory.accounts}")
            Text("• Movimientos: ${inventory.transactions}")
            Text("• Clientes: ${inventory.clients}")
            Text("• Proveedores: ${inventory.suppliers}")
        }
        warning?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun StrengthMeter(strength: BackupPasswordPolicy.Strength) {
    val progress = when (strength) {
        BackupPasswordPolicy.Strength.WEAK -> 0.33f
        BackupPasswordPolicy.Strength.MEDIUM -> 0.66f
        BackupPasswordPolicy.Strength.STRONG -> 1f
    }
    val color = when (strength) {
        BackupPasswordPolicy.Strength.WEAK -> MaterialTheme.colorScheme.error
        BackupPasswordPolicy.Strength.MEDIUM -> Color(0xFFB8860B)
        BackupPasswordPolicy.Strength.STRONG -> MaterialTheme.colorScheme.primary
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Fortaleza: ${BackupPasswordPolicy.strengthLabel(strength)}",
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
