package com.paycontrol.app.ui.screens.suppliers

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
fun SuppliersScreen(
    viewModel: SuppliersViewModel,
    contactsRepository: ContactsRepository
) {
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val pagingItems = viewModel.pagedSuppliers.collectAsLazyPagingItems()
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
            title = { Text(stringResource(R.string.suppliers_delete_title)) },
            text = {
                Text(stringResource(R.string.suppliers_delete_body, selectedSupplier.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteSelectedSupplier()
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
                title = stringResource(R.string.suppliers_title),
                subtitle = stringResource(R.string.suppliers_subtitle)
            )
        }

        item {
            SoftPanel {
                Text(stringResource(R.string.suppliers_new), style = MaterialTheme.typography.titleMedium)
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
                    onClick = viewModel::createSupplier,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.suppliers_add))
                }
            }
        }

        item {
            SoftPanel {
                Text(stringResource(R.string.suppliers_register_payment), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = selectedSupplier?.let {
                        stringResource(
                            R.string.suppliers_pay_hint_selected,
                            it.name,
                            Money.format(it.totalPaid)
                        )
                    } ?: stringResource(R.string.suppliers_pay_hint_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AccountPickerField(
                    accounts = accounts,
                    selectedAccountId = state.selectedAccountId,
                    onAccountSelected = viewModel::onAccountSelected,
                    label = stringResource(R.string.suppliers_account_source)
                )
                val selectedAccount = accounts.firstOrNull { it.id == state.selectedAccountId }
                    ?: accounts.firstOrNull()
                val paymentCents = Money.parseToCents(state.paymentAmount)
                if (selectedAccount != null) {
                    Text(
                        stringResource(
                            R.string.suppliers_will_debit,
                            selectedAccount.name,
                            Money.format(selectedAccount.balance)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (paymentCents != null && paymentCents > selectedAccount.balance) {
                        Text(
                            stringResource(R.string.suppliers_insufficient_balance),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                MoneyAmountField(
                    value = state.paymentAmount,
                    onValueChange = viewModel::onPaymentAmountChange,
                    label = stringResource(R.string.suppliers_payment_amount),
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
                            state.isPaying -> stringResource(R.string.action_registering)
                            selectedSupplier == null -> stringResource(R.string.suppliers_select)
                            insufficient -> stringResource(R.string.suppliers_insufficient)
                            else -> stringResource(R.string.suppliers_register_payment)
                        }
                    )
                }
                StatusMessage(state.errorMessage, state.successMessage)
            }
        }

        item {
            SoftPanel {
                Text(stringResource(R.string.suppliers_manage), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = selectedSupplier?.let {
                        stringResource(R.string.suppliers_editing, it.name)
                    } ?: stringResource(R.string.suppliers_manage_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = state.editName,
                    onValueChange = viewModel::onEditNameChange,
                    label = { Text(stringResource(R.string.label_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = selectedSupplier != null && !state.isBusy
                )
                OutlinedTextField(
                    value = state.editPhone,
                    onValueChange = viewModel::onEditPhoneChange,
                    label = { Text(stringResource(R.string.label_phone)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = selectedSupplier != null && !state.isBusy
                )
                OutlinedButton(
                    onClick = viewModel::saveSupplierEdits,
                    enabled = selectedSupplier != null && !state.isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_save_changes))
                }
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    enabled = selectedSupplier != null && !state.isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.suppliers_delete_button),
                        color = if (selectedSupplier != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }

        if (suppliers.isEmpty()) {
            item {
                SoftPanel {
                    Text(
                        stringResource(R.string.suppliers_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.id },
            contentType = { "supplier" }
        ) { index ->
            val supplier = pagingItems[index] ?: return@items
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
                    Text(stringResource(R.string.suppliers_total_paid, Money.format(supplier.totalPaid)))
                }
            }
        }
    }
}
