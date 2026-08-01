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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paycontrol.app.R
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
                title = { Text(stringResource(R.string.pin_lock_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
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
                title = stringResource(R.string.pin_settings_title),
                subtitle = if (pinEnabled) {
                    stringResource(R.string.pin_settings_active_body)
                } else {
                    stringResource(R.string.pin_settings_inactive_body)
                }
            )

            SoftPanel {
                Text(
                    text = if (pinEnabled) {
                        stringResource(R.string.pin_status_on)
                    } else {
                        stringResource(R.string.pin_status_off)
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.pin_hash_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!pinEnabled) {
                SoftPanel {
                    Text(
                        stringResource(R.string.pin_activate),
                        style = MaterialTheme.typography.titleMedium
                    )
                    PinField(
                        label = stringResource(R.string.pin_new_label),
                        value = state.newPin,
                        onValueChange = viewModel::onNewPinChange
                    )
                    PinField(
                        label = stringResource(R.string.pin_confirm_label),
                        value = state.confirmPin,
                        onValueChange = viewModel::onConfirmPinChange
                    )
                    Button(
                        onClick = viewModel::enablePin,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isBusy
                    ) {
                        Text(
                            if (state.isBusy) {
                                stringResource(R.string.action_saving)
                            } else {
                                stringResource(R.string.pin_activate)
                            }
                        )
                    }
                }
            } else {
                SoftPanel {
                    Text(
                        stringResource(R.string.pin_change),
                        style = MaterialTheme.typography.titleMedium
                    )
                    PinField(
                        label = stringResource(R.string.pin_current_label),
                        value = state.currentPin,
                        onValueChange = viewModel::onCurrentPinChange
                    )
                    PinField(
                        label = stringResource(R.string.pin_new_label),
                        value = state.newPin,
                        onValueChange = viewModel::onNewPinChange
                    )
                    PinField(
                        label = stringResource(R.string.pin_confirm_new_label),
                        value = state.confirmPin,
                        onValueChange = viewModel::onConfirmPinChange
                    )
                    Button(
                        onClick = viewModel::changePin,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isBusy
                    ) {
                        Text(
                            if (state.isBusy) {
                                stringResource(R.string.action_saving)
                            } else {
                                stringResource(R.string.pin_change)
                            }
                        )
                    }
                }

                SoftPanel {
                    Text(
                        stringResource(R.string.pin_disable),
                        style = MaterialTheme.typography.titleMedium
                    )
                    PinField(
                        label = stringResource(R.string.pin_current_label),
                        value = state.disablePin,
                        onValueChange = viewModel::onDisablePinChange
                    )
                    OutlinedButton(
                        onClick = viewModel::disablePin,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isBusy
                    ) {
                        Text(stringResource(R.string.pin_disable))
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
