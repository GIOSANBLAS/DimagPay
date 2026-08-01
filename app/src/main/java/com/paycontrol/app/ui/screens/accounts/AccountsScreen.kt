package com.paycontrol.app.ui.screens.accounts

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paycontrol.app.data.local.entity.AccountEntity
import com.paycontrol.app.domain.model.AccountType
import com.paycontrol.app.domain.util.Money
import com.paycontrol.app.ui.components.AccountPickerField
import com.paycontrol.app.ui.components.MoneyAmountField
import com.paycontrol.app.ui.components.SectionTitle
import com.paycontrol.app.ui.components.SoftPanel
import com.paycontrol.app.ui.components.StatusMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    onBack: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selected = accounts.firstOrNull { it.id == state.selectedAccountId }

    if (state.showDeleteConfirm && selected != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirm,
            title = { Text("Eliminar cuenta") },
            text = {
                Text(
                    "¿Eliminar «${selected.name}»? Solo es posible si no tiene movimientos."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteConfirm) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cuentas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "title") {
                SectionTitle(
                    title = "Cuentas",
                    subtitle = "Efectivo, banco y otras formas de pago."
                )
            }

            item(key = "create") {
                SoftPanel {
                    Text("Nueva cuenta", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    AccountTypeDropdown(
                        selectedType = state.type,
                        onTypeSelected = viewModel::onTypeChange,
                        label = "Tipo"
                    )
                    MoneyAmountField(
                        value = state.initialBalanceInput,
                        onValueChange = viewModel::onInitialBalanceChange,
                        label = "Saldo inicial (opcional)",
                        placeholder = "0.00"
                    )
                    Button(
                        onClick = viewModel::createAccount,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.isSaving) "Guardando…" else "Crear cuenta")
                    }
                    if (selected == null) {
                        StatusMessage(state.errorMessage, state.successMessage)
                    }
                }
            }

            item(key = "transfer") {
                SoftPanel {
                    Text("Transferir entre cuentas", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Mueve saldo de una cuenta a otra sin afectar ingresos ni gastos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AccountPickerField(
                        accounts = accounts,
                        selectedAccountId = state.transferFromAccountId,
                        onAccountSelected = viewModel::onTransferFromAccountSelected,
                        label = "Cuenta origen"
                    )
                    AccountPickerField(
                        accounts = accounts,
                        selectedAccountId = state.transferToAccountId,
                        onAccountSelected = viewModel::onTransferToAccountSelected,
                        label = "Cuenta destino"
                    )
                    MoneyAmountField(
                        value = state.transferAmountInput,
                        onValueChange = viewModel::onTransferAmountChange,
                        label = "Monto",
                        placeholder = "0.00"
                    )
                    val canTransfer = accounts.size >= 2 && !state.isSaving
                    Button(
                        onClick = viewModel::transferBetweenAccounts,
                        enabled = canTransfer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            when {
                                state.isSaving -> "Transferiendo…"
                                accounts.size < 2 -> "Necesitas al menos 2 cuentas"
                                else -> "Transferir"
                            }
                        )
                    }
                    StatusMessage(state.errorMessage, state.successMessage)
                }
            }

            if (selected != null) {
                item(key = "edit") {
                    SoftPanel {
                        Text("Editar cuenta", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Saldo actual: ${Money.format(selected.balance)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = state.editName,
                            onValueChange = viewModel::onEditNameChange,
                            label = { Text("Nombre") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        AccountTypeDropdown(
                            selectedType = state.editType,
                            onTypeSelected = viewModel::onEditTypeChange,
                            label = "Tipo"
                        )
                        Button(
                            onClick = viewModel::updateAccount,
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Guardar cambios")
                        }
                        OutlinedButton(
                            onClick = viewModel::requestDelete,
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Eliminar cuenta")
                        }
                        TextButton(
                            onClick = viewModel::clearSelection,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancelar selección")
                        }
                        StatusMessage(state.errorMessage, state.successMessage)
                    }
                }
            }

            item(key = "list_title") {
                Text("Tus cuentas", style = MaterialTheme.typography.titleMedium)
            }

            if (accounts.isEmpty()) {
                item(key = "empty") {
                    SoftPanel {
                        Text(
                            "Aún no hay cuentas. Crea una para empezar a registrar movimientos.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(
                items = accounts,
                key = { it.id },
                contentType = { "account" }
            ) { account ->
                AccountListItem(
                    account = account,
                    selected = state.selectedAccountId == account.id,
                    onClick = { viewModel.onAccountSelected(account.id) }
                )
            }
        }
    }
}

@Composable
private fun AccountListItem(
    account: AccountEntity,
    selected: Boolean,
    onClick: () -> Unit
) {
    SoftPanel(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (selected) {
                    Modifier.border(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(20.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    account.name,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    account.type,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                Money.format(account.balance),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AccountTypeDropdown(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    label: String
) {
    var showPicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedType,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            trailingIcon = {
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showPicker = true }
        )
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    AccountType.all.forEach { type ->
                        val isSelected = type == selectedType
                        Text(
                            text = type,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onTypeSelected(type)
                                    showPicker = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
