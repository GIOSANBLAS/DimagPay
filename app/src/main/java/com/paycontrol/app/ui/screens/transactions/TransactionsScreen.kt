package com.paycontrol.app.ui.screens.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paycontrol.app.R
import com.paycontrol.app.data.local.entity.AccountEntity
import com.paycontrol.app.data.local.entity.TransactionEntity
import com.paycontrol.app.domain.model.DefaultCategories
import com.paycontrol.app.domain.model.TransactionType
import com.paycontrol.app.domain.util.Money
import com.paycontrol.app.ui.components.AccountPickerField
import com.paycontrol.app.ui.components.MoneyAmountField
import com.paycontrol.app.ui.components.SectionTitle
import com.paycontrol.app.ui.components.SoftPanel
import com.paycontrol.app.ui.components.StatusMessage

@Composable
fun TransactionsScreen(viewModel: TransactionsViewModel) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val pagingItems = viewModel.pagedTransactions.collectAsLazyPagingItems()
    val pendingDelete = form.pendingDelete
    val resources = LocalContext.current.resources

    LaunchedEffect(Unit) {
        viewModel.ensureDefaultAccount()
    }

    LaunchedEffect(accounts, form.accountId) {
        if (form.accountId == null) {
            accounts.firstOrNull()?.id?.let(viewModel::onAccountSelected)
        }
    }

    val categories = remember(form.type, resources) {
        if (form.type == TransactionType.INCOME) {
            DefaultCategories.income(resources)
        } else {
            DefaultCategories.expense(resources)
        }
    }

    if (pendingDelete != null) {
        val typeLabel = when (pendingDelete.type) {
            TransactionType.INCOME -> stringResource(R.string.type_income)
            TransactionType.EXPENSE -> stringResource(R.string.type_expense)
            else -> pendingDelete.type
        }
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirm,
            title = { Text(stringResource(R.string.transactions_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.transactions_delete_body,
                        typeLabel,
                        pendingDelete.category,
                        Money.format(pendingDelete.amount)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeletePending) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteConfirm) {
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
        item(key = "title") {
            SectionTitle(
                title = stringResource(R.string.transactions_title),
                subtitle = stringResource(R.string.transactions_subtitle)
            )
        }

        item(key = "form") {
            TransactionFormPanel(
                form = form,
                accounts = accounts,
                categories = categories,
                onTypeChange = viewModel::onTypeChange,
                onAccountSelected = viewModel::onAccountSelected,
                onAmountChange = viewModel::onAmountChange,
                onCategoryChange = viewModel::onCategoryChange,
                onNoteChange = viewModel::onNoteChange,
                onSave = viewModel::saveTransaction
            )
        }

        item(key = "history_title") {
            Text(
                stringResource(R.string.transactions_history),
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.id },
            contentType = { "tx" }
        ) { index ->
            val tx = pagingItems[index]
            if (tx != null) {
                TransactionHistoryItem(
                    tx = tx,
                    isDeleting = form.deletingId == tx.id,
                    onDeleteClick = { viewModel.requestDelete(tx) }
                )
            }
        }
    }
}

@Composable
private fun TransactionFormPanel(
    form: TransactionFormState,
    accounts: List<AccountEntity>,
    categories: List<String>,
    onTypeChange: (String) -> Unit,
    onAccountSelected: (Long) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit
) {
    var categoryPicker by remember { mutableStateOf(false) }

    SoftPanel {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = form.type == TransactionType.INCOME,
                onClick = { onTypeChange(TransactionType.INCOME) },
                label = { Text(stringResource(R.string.type_income)) }
            )
            FilterChip(
                selected = form.type == TransactionType.EXPENSE,
                onClick = { onTypeChange(TransactionType.EXPENSE) },
                label = { Text(stringResource(R.string.type_expense)) }
            )
        }

        AccountPickerField(
            accounts = accounts,
            selectedAccountId = form.accountId,
            onAccountSelected = onAccountSelected,
            label = stringResource(R.string.label_account)
        )

        MoneyAmountField(
            value = form.amountInput,
            onValueChange = onAmountChange,
            label = stringResource(R.string.label_amount)
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = form.category,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text(stringResource(R.string.label_category)) },
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
                    .clickable { categoryPicker = true }
            )
        }

        if (categoryPicker) {
            AlertDialog(
                onDismissRequest = { categoryPicker = false },
                title = { Text(stringResource(R.string.label_category)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        categories.forEach { category ->
                            val selected = category == form.category
                            Text(
                                text = category,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCategoryChange(category)
                                        categoryPicker = false
                                    }
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { categoryPicker = false }) {
                        Text(stringResource(R.string.action_close))
                    }
                }
            )
        }

        OutlinedTextField(
            value = form.note,
            onValueChange = onNoteChange,
            label = { Text(stringResource(R.string.label_note_optional)) },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onSave,
            enabled = !form.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (form.isSaving) {
                    stringResource(R.string.action_saving)
                } else {
                    stringResource(R.string.transactions_save)
                }
            )
        }
        StatusMessage(form.errorMessage, form.successMessage)
    }
}

@Composable
private fun TransactionHistoryItem(
    tx: TransactionEntity,
    isDeleting: Boolean,
    onDeleteClick: () -> Unit
) {
    val typeLabel = when (tx.type) {
        TransactionType.INCOME -> stringResource(R.string.type_income)
        TransactionType.EXPENSE -> stringResource(R.string.type_expense)
        else -> tx.type
    }
    val subtitle = remember(tx.amount, tx.note) {
        Money.format(tx.amount) + if (tx.note.isNotBlank()) " — ${tx.note}" else ""
    }
    SoftPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("$typeLabel · ${tx.category}", fontWeight = FontWeight.Medium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDeleteClick,
                enabled = !isDeleting
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.transactions_delete_cd),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
