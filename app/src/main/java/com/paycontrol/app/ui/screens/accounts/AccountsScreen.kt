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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paycontrol.app.R
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
            title = { Text(stringResource(R.string.accounts_delete_title)) },
            text = {
                Text(stringResource(R.string.accounts_delete_body, selected.name))
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteConfirm) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.accounts_title)) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "title") {
                SectionTitle(
                    title = stringResource(R.string.accounts_title),
                    subtitle = stringResource(R.string.accounts_subtitle)
                )
            }

            item(key = "create") {
                SoftPanel {
                    Text(stringResource(R.string.accounts_new), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = { Text(stringResource(R.string.label_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    AccountTypeDropdown(
                        selectedType = state.type,
                        onTypeSelected = viewModel::onTypeChange,
                        label = stringResource(R.string.label_type)
                    )
                    MoneyAmountField(
                        value = state.initialBalanceInput,
                        onValueChange = viewModel::onInitialBalanceChange,
                        label = stringResource(R.string.accounts_initial_balance_optional),
                        placeholder = "0.00"
                    )
                    Button(
                        onClick = viewModel::createAccount,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (state.isSaving) {
                                stringResource(R.string.action_saving)
                            } else {
                                stringResource(R.string.accounts_create)
                            }
                        )
                    }
                    if (selected == null) {
                        StatusMessage(state.errorMessage, state.successMessage)
                    }
                }
            }

            item(key = "transfer") {
                SoftPanel {
                    Text(stringResource(R.string.accounts_transfer), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.accounts_transfer_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AccountPickerField(
                        accounts = accounts,
                        selectedAccountId = state.transferFromAccountId,
                        onAccountSelected = viewModel::onTransferFromAccountSelected,
                        label = stringResource(R.string.accounts_transfer_from)
                    )
                    AccountPickerField(
                        accounts = accounts,
                        selectedAccountId = state.transferToAccountId,
                        onAccountSelected = viewModel::onTransferToAccountSelected,
                        label = stringResource(R.string.accounts_transfer_to)
                    )
                    MoneyAmountField(
                        value = state.transferAmountInput,
                        onValueChange = viewModel::onTransferAmountChange,
                        label = stringResource(R.string.label_amount),
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
                                state.isSaving -> stringResource(R.string.accounts_transferring)
                                accounts.size < 2 -> stringResource(R.string.accounts_need_two)
                                else -> stringResource(R.string.accounts_transfer_action)
                            }
                        )
                    }
                    StatusMessage(state.errorMessage, state.successMessage)
                }
            }

            if (selected != null) {
                item(key = "edit") {
                    SoftPanel {
                        Text(stringResource(R.string.accounts_edit), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.accounts_current_balance, Money.format(selected.balance)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = state.editName,
                            onValueChange = viewModel::onEditNameChange,
                            label = { Text(stringResource(R.string.label_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        AccountTypeDropdown(
                            selectedType = state.editType,
                            onTypeSelected = viewModel::onEditTypeChange,
                            label = stringResource(R.string.label_type)
                        )
                        Button(
                            onClick = viewModel::updateAccount,
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.action_save_changes))
                        }
                        OutlinedButton(
                            onClick = viewModel::requestDelete,
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.accounts_delete_button))
                        }
                        TextButton(
                            onClick = viewModel::clearSelection,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.accounts_cancel_selection))
                        }
                        StatusMessage(state.errorMessage, state.successMessage)
                    }
                }
            }

            item(key = "list_title") {
                Text(stringResource(R.string.accounts_yours), style = MaterialTheme.typography.titleMedium)
            }

            if (accounts.isEmpty()) {
                item(key = "empty") {
                    SoftPanel {
                        Text(
                            stringResource(R.string.accounts_empty),
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
    val resources = LocalContext.current.resources
    val typeLabel = remember(account.type, resources) {
        val index = AccountType.all.indexOf(account.type)
        if (index >= 0) {
            AccountType.all(resources)[index]
        } else {
            account.type
        }
    }
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
                    typeLabel,
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
    val resources = LocalContext.current.resources
    val persistedTypes = AccountType.all
    val displayTypes = remember(resources) { AccountType.all(resources) }
    val displaySelectedType = remember(selectedType, resources) {
        val index = persistedTypes.indexOf(selectedType)
        if (index >= 0) displayTypes[index] else selectedType
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = displaySelectedType,
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
                    persistedTypes.forEachIndexed { index, persistedType ->
                        val displayType = displayTypes[index]
                        val isSelected = persistedType == selectedType
                        Text(
                            text = displayType,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onTypeSelected(persistedType)
                                    showPicker = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}
