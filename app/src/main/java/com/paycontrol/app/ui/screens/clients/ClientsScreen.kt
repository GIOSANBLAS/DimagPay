package com.paycontrol.app.ui.screens.clients

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.paycontrol.app.data.contacts.ContactsRepository
import com.paycontrol.app.domain.util.Money
import com.paycontrol.app.ui.components.AccountPickerField
import com.paycontrol.app.ui.components.ContactPickerSheet
import com.paycontrol.app.ui.components.MoneyAmountField
import com.paycontrol.app.ui.components.SectionTitle
import com.paycontrol.app.ui.components.SoftPanel
import com.paycontrol.app.ui.components.StatusMessage

@Composable
fun ClientsScreen(
    viewModel: ClientsViewModel,
    contactsRepository: ContactsRepository
) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val pagingItems = viewModel.pagedClients.collectAsLazyPagingItems()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val receivables by viewModel.totalReceivables.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedClient = clients.firstOrNull { it.id == state.selectedClientId }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (state.showContactPicker) {
        ContactPickerSheet(
            contactsRepository = contactsRepository,
            onDismiss = viewModel::closeContactPicker,
            onContactSelected = viewModel::applyContact
        )
    }

    if (showDeleteConfirm && selectedClient != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar cliente") },
            text = {
                Text("¿Eliminar a «${selectedClient.name}»? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteSelectedClient()
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitle(
                title = "Clientes",
                subtitle = "Por cobrar · ${Money.format(receivables)}"
            )
        }

        item {
            SoftPanel {
                Text("Nuevo cliente", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = viewModel::onPhoneChange,
                    label = { Text("Teléfono") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                MoneyAmountField(
                    value = state.debtInput,
                    onValueChange = viewModel::onDebtInputChange,
                    label = "Deuda inicial (opcional)",
                    placeholder = "0.00"
                )
                FilledTonalButton(
                    onClick = viewModel::openContactPicker,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Contacts, contentDescription = "Importar contacto")
                    Text("  Importar contacto")
                }
                Button(
                    onClick = viewModel::createClient,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Agregar cliente")
                }
            }
        }

        item {
            SoftPanel {
                Text("Abonar a deuda", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = selectedClient?.let {
                        "Cliente: ${it.name} · Deuda ${Money.format(it.totalDebt)}"
                    } ?: "Toca un cliente abajo para abonar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AccountPickerField(
                    accounts = accounts,
                    selectedAccountId = state.selectedAccountId,
                    onAccountSelected = viewModel::onAccountSelected,
                    label = "Cuenta destino (efectivo/banco)"
                )
                val selectedAccount = accounts.firstOrNull { it.id == state.selectedAccountId }
                    ?: accounts.firstOrNull()
                if (selectedAccount != null) {
                    Text(
                        "Ingresará a ${selectedAccount.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                MoneyAmountField(
                    value = state.paymentInput,
                    onValueChange = viewModel::onPaymentInputChange,
                    label = "Monto del abono",
                    enabled = selectedClient != null && (selectedClient.totalDebt > 0L)
                )
                val canPay = selectedClient != null &&
                    selectedClient.totalDebt > 0L &&
                    !state.isPaying &&
                    accounts.isNotEmpty()
                OutlinedButton(
                    onClick = viewModel::applyPayment,
                    enabled = canPay,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            state.isPaying -> "Registrando…"
                            selectedClient == null -> "Selecciona un cliente"
                            selectedClient.totalDebt <= 0L -> "Sin deuda pendiente"
                            else -> "Registrar abono"
                        }
                    )
                }
                StatusMessage(state.errorMessage, state.successMessage)
            }
        }

        item {
            SoftPanel {
                Text("Gestionar cliente", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = selectedClient?.let { "Editando: ${it.name}" }
                        ?: "Toca un cliente abajo para editar, agregar deuda o eliminar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = state.editName,
                    onValueChange = viewModel::onEditNameChange,
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = selectedClient != null && !state.isBusy
                )
                OutlinedTextField(
                    value = state.editPhone,
                    onValueChange = viewModel::onEditPhoneChange,
                    label = { Text("Teléfono") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = selectedClient != null && !state.isBusy
                )
                OutlinedButton(
                    onClick = viewModel::saveClientEdits,
                    enabled = selectedClient != null && !state.isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guardar cambios")
                }
                MoneyAmountField(
                    value = state.addDebtInput,
                    onValueChange = viewModel::onAddDebtInputChange,
                    label = "Agregar deuda",
                    enabled = selectedClient != null && !state.isBusy
                )
                FilledTonalButton(
                    onClick = viewModel::addDebt,
                    enabled = selectedClient != null && !state.isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isBusy) "Procesando…" else "Agregar deuda")
                }
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    enabled = selectedClient != null && !state.isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Eliminar cliente",
                        color = if (selectedClient != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                StatusMessage(state.errorMessage, state.successMessage)
            }
        }

        if (clients.isEmpty()) {
            item {
                SoftPanel {
                    Text(
                        "Aún no hay clientes. Crea uno o impórtalo desde contactos.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.id },
            contentType = { "client" }
        ) { index ->
            val client = pagingItems[index] ?: return@items
            val selected = state.selectedClientId == client.id
            val hasDebt = client.totalDebt > 0L
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
                    .clickable { viewModel.onClientSelected(client.id) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        client.name,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = if (hasDebt) "PENDIENTE" else "AL CORRIENTE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hasDebt) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
                if (client.phone.isNotBlank()) {
                    Text(client.phone, style = MaterialTheme.typography.bodySmall)
                }
                Text("Deuda: ${Money.format(client.totalDebt)}")
            }
        }
    }
}
