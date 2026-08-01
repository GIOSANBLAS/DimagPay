package com.paycontrol.app.ui.screens.lock

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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paycontrol.app.ui.components.SectionTitle
import com.paycontrol.app.ui.components.SoftPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSettingsScreen(
    viewModel: PinSettingsViewModel,
    onBack: () -> Unit
) {
    val pinEnabled by viewModel.pinEnabled.collectAsStateWithLifecycle()
    val state by viewModel.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bloqueo por PIN") },
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
            SectionTitle(
                title = "PIN de la app",
                subtitle = if (pinEnabled) {
                    "El bloqueo está activo. Puedes cambiar o desactivar el PIN."
                } else {
                    "Protege DimagPay con un PIN de 4 a 8 dígitos. Se guarda cifrado en el dispositivo."
                }
            )

            SoftPanel {
                Text(
                    text = if (pinEnabled) "Estado: activado" else "Estado: desactivado",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "El PIN nunca se guarda en texto plano (solo un hash con sal).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!pinEnabled) {
                SoftPanel {
                    Text("Activar PIN", style = MaterialTheme.typography.titleMedium)
                    PinField(
                        label = "Nuevo PIN",
                        value = state.newPin,
                        onValueChange = viewModel::onNewPinChange
                    )
                    PinField(
                        label = "Confirmar PIN",
                        value = state.confirmPin,
                        onValueChange = viewModel::onConfirmPinChange
                    )
                    Button(
                        onClick = viewModel::enablePin,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isBusy
                    ) {
                        Text(if (state.isBusy) "Guardando…" else "Activar PIN")
                    }
                }
            } else {
                SoftPanel {
                    Text("Cambiar PIN", style = MaterialTheme.typography.titleMedium)
                    PinField(
                        label = "PIN actual",
                        value = state.currentPin,
                        onValueChange = viewModel::onCurrentPinChange
                    )
                    PinField(
                        label = "Nuevo PIN",
                        value = state.newPin,
                        onValueChange = viewModel::onNewPinChange
                    )
                    PinField(
                        label = "Confirmar nuevo PIN",
                        value = state.confirmPin,
                        onValueChange = viewModel::onConfirmPinChange
                    )
                    Button(
                        onClick = viewModel::changePin,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isBusy
                    ) {
                        Text(if (state.isBusy) "Guardando…" else "Cambiar PIN")
                    }
                }

                SoftPanel {
                    Text("Desactivar PIN", style = MaterialTheme.typography.titleMedium)
                    PinField(
                        label = "PIN actual",
                        value = state.disablePin,
                        onValueChange = viewModel::onDisablePinChange
                    )
                    OutlinedButton(
                        onClick = viewModel::disablePin,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isBusy
                    ) {
                        Text("Desactivar bloqueo")
                    }
                }
            }

            state.message?.let { message ->
                Text(
                    text = message,
                    color = if (state.messageIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun PinField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
    )
}
