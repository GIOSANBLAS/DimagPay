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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paycontrol.app.R
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
            title = { Text(stringResource(R.string.clients_delete_title)) },
            text = {
                Text(stringResource(R.string.clients_delete_body, selectedClient.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteSelectedClient()
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
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
                title = stringResource(R.string.clients_title),
                subtitle = stringResource(R.string.clients_subtitle_receivables, Money.format(receivables))
            )
        }

        item {
            SoftPanel {
                Text(stringResource(R.string.clients_new), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text(stringResource(R.string.label_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = viewModel::onPhoneChange,
                    label = { Text(stringResource(R.string.label_phone)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                MoneyAmountField(
                    value = state.debtInput,
                    onValueChange = viewModel::onDebtInputChange,
                    label = stringResource(R.string.clients_initial_debt_optional),
                    placeholder = "0.00"
                )
                FilledTonalButton(
                    onClick = viewModel::openContactPicker,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Outlined.Contacts,
                        contentDescription = stringResource(R.string.import_contact)
                    )
                    Text("  ${stringResource(R.string.import_contact)}")
                }
                Button(
                    onClick = viewModel::createClient,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.clients_add))
                }
            }
        }

        item {
            SoftPanel {
                Text(stringResource(R.string.clients_pay_debt), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = selectedClient?.let {
                        stringResource(
                            R.string.clients_pay_hint_selected,
                            it.name,
                            Money.format(it.totalDebt)
                        )
                    } ?: stringResource(R.string.clients_pay_hint_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AccountPickerField(
                    accounts = accounts,
                    selectedAccountId = state.selectedAccountId,
                    onAccountSelected = viewModel::onAccountSelected,
                    label = stringResource(R.string.clients_account_dest)
                )
                val selectedAccount = accounts.firstOrNull { it.id == state.selectedAccountId }
                    ?: accounts.firstOrNull()
                if (selectedAccount != null) {
                    Text(
                        stringResource(R.string.clients_will_credit, selectedAccount.name),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                MoneyAmountField(
                    value = state.paymentInput,
                    onValueChange = viewModel::onPaymentInputChange,
                    label = stringResource(R.string.clients_payment_amount),
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
                            state.isPaying -> stringResource(R.string.action_registering)
                            selectedClient == null -> stringResource(R.string.clients_select_client)
                            selectedClient.totalDebt <= 0L -> stringResource(R.string.clients_no_debt)
                            else -> stringResource(R.string.clients_register_payment)
                        }
                    )
                }
                StatusMessage(state.errorMessage, state.successMessage)
            }
        }

        item {
            SoftPanel {
                Text(stringResource(R.string.clients_manage), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = selectedClient?.let {
                        stringResource(R.string.clients_editing, it.name)
                    } ?: stringResource(R.string.clients_manage_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = state.editName,
                    onValueChange = viewModel::onEditNameChange,
                    label = { Text(stringResource(R.string.label_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = selectedClient != null && !state.isBusy
                )
                OutlinedTextField(
                    value = state.editPhone,
                    onValueChange = viewModel::onEditPhoneChange,
                    label = { Text(stringResource(R.string.label_phone)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = selectedClient != null && !state.isBusy
                )
                OutlinedButton(
                    onClick = viewModel::saveClientEdits,
                    enabled = selectedClient != null && !state.isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_save_changes))
                }
                MoneyAmountField(
                    value = state.addDebtInput,
                    onValueChange = viewModel::onAddDebtInputChange,
                    label = stringResource(R.string.clients_add_debt),
                    enabled = selectedClient != null && !state.isBusy
                )
                FilledTonalButton(
                    onClick = viewModel::addDebt,
                    enabled = selectedClient != null && !state.isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (state.isBusy) {
                            stringResource(R.string.action_processing)
                        } else {
                            stringResource(R.string.clients_add_debt)
                        }
                    )
                }
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    enabled = selectedClient != null && !state.isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.clients_delete_button),
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
                        stringResource(R.string.clients_empty),
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
                        text = if (hasDebt) {
                            stringResource(R.string.clients_status_pending)
                        } else {
                            stringResource(R.string.clients_status_current)
                        },
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
                Text(stringResource(R.string.clients_debt, Money.format(client.totalDebt)))
            }
        }
    }
}
