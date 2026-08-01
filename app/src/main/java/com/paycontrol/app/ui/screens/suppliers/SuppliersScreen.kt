package com.paycontrol.app.ui.screens.suppliers

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun SuppliersScreen(
    viewModel: SuppliersViewModel,
    contactsRepository: ContactsRepository
) {
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedSupplier = suppliers.firstOrNull { it.id == state.selectedSupplierId }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (state.showContactPicker) {
        ContactPickerSheet(
            contactsRepository = contactsRepository,
            onDismiss = viewModel::closeContactPicker,
            onContactSelected = viewModel::applyContact
        )
    }

    if (showDeleteConfirm && selectedSupplier != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar proveedor") },
            text = {
                Text("¿Eliminar a «${selectedSupplier.name}»? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteSelectedSupplier()
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
                title = "Proveedores",
                subtitle = "Pagos vinculados a tu saldo"
            )
        }

        item {
            SoftPanel {
                Text("Nuevo proveedor", style = MaterialTheme.typography.titleMedium)
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
                FilledTonalButton(
                    onClick = viewModel::openContactPicker,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Contacts, contentDescription = "Importar contacto")
                    Text("  Importar contacto")
                }
                Button(
                    onClick = viewModel::createSupplier,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Agregar proveedor")
                }
            }
        }

        item {
            SoftPanel {
                Text("Registrar pago", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = selectedSupplier?.let {
                        "Proveedor: ${it.name} · Pagado ${Money.format(it.totalPaid)}"
                    } ?: "Toca un proveedor abajo para pagar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AccountPickerField(
                    accounts = accounts,
                    selectedAccountId = state.selectedAccountId,
                    onAccountSelected = viewModel::onAccountSelected,
                    label = "Cuenta de origen (efectivo/banco)"
                )
                val selectedAccount = accounts.firstOrNull { it.id == state.selectedAccountId }
                    ?: accounts.firstOrNull()
                val paymentCents = Money.parseToCents(state.paymentAmount)
                if (selectedAccount != null) {
                    Text(
                        "Se descontará de ${selectedAccount.name} · disponible ${Money.format(selectedAccount.balance)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (paymentCents != null && paymentCents > selectedAccount.balance) {
                        Text(
                            "Saldo insuficiente para este monto",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                MoneyAmountField(
                    value = state.paymentAmount,
                    onValueChange = viewModel::onPaymentAmountChange,
                    label = "Monto del pago",
                    enabled = selectedSupplier != null
                )
                val insufficient = selectedAccount != null &&
                    paymentCents != null &&
                    paymentCents > selectedAccount.balance
                val canPay = selectedSupplier != null &&
                    !state.isPaying &&
                    accounts.isNotEmpty() &&
                    !insufficient
                OutlinedButton(
                    onClick = viewModel::registerPayment,
                    enabled = canPay,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            state.isPaying -> "Registrando…"
                            selectedSupplier == null -> "Selecciona un proveedor"
                            insufficient -> "Saldo insuficiente"
                            else -> "Registrar pago"
                        }
                    )
                }
                StatusMessage(state.errorMessage, state.successMessage)
            }
        }

        item {
            SoftPanel {
                Text("Gestionar proveedor", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = selectedSupplier?.let { "Editando: ${it.name}" }
                        ?: "Toca un proveedor abajo para editar o eliminar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = state.editName,
                    onValueChange = viewModel::onEditNameChange,
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = selectedSupplier != null && !state.isBusy
                )
                OutlinedTextField(
                    value = state.editPhone,
                    onValueChange = viewModel::onEditPhoneChange,
                    label = { Text("Teléfono") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = selectedSupplier != null && !state.isBusy
                )
                OutlinedButton(
                    onClick = viewModel::saveSupplierEdits,
                    enabled = selectedSupplier != null && !state.isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guardar cambios")
                }
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    enabled = selectedSupplier != null && !state.isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Eliminar proveedor",
                        color = if (selectedSupplier != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                StatusMessage(state.errorMessage, state.successMessage)
            }
        }

        if (suppliers.isEmpty()) {
            item {
                SoftPanel {
                    Text(
                        "Aún no hay proveedores. Crea uno o impórtalo desde contactos.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(
            items = suppliers,
            key = { it.id },
            contentType = { "supplier" }
        ) { supplier ->
            val selected = state.selectedSupplierId == supplier.id
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
                    .clickable { viewModel.onSupplierSelected(supplier.id) }
            ) {
                Column {
                    Text(
                        supplier.name,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (supplier.phone.isNotBlank()) {
                        Text(supplier.phone, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Total pagado: ${Money.format(supplier.totalPaid)}")
                }
            }
        }
    }
}
