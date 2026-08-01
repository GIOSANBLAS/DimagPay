package com.paycontrol.app.ui.screens.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paycontrol.app.data.local.entity.AccountEntity
import com.paycontrol.app.data.local.entity.TransactionEntity
import com.paycontrol.app.domain.model.DefaultCategories
import com.paycontrol.app.domain.model.TransactionType
import com.paycontrol.app.domain.util.Money
import com.paycontrol.app.ui.components.MoneyAmountField
import com.paycontrol.app.ui.components.SectionTitle
import com.paycontrol.app.ui.components.SoftPanel
import com.paycontrol.app.ui.components.StatusMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(viewModel: TransactionsViewModel) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }
    val pendingDeleteTx = transactions.firstOrNull { it.id == pendingDeleteId }

    LaunchedEffect(Unit) {
        viewModel.ensureDefaultAccount()
    }

    LaunchedEffect(accounts, form.accountId) {
        if (form.accountId == null) {
            accounts.firstOrNull()?.id?.let(viewModel::onAccountSelected)
        }
    }

    val categories = remember(form.type) {
        if (form.type == TransactionType.INCOME) DefaultCategories.income
        else DefaultCategories.expense
    }

    if (pendingDeleteTx != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Eliminar movimiento") },
            text = {
                Text(
                    "¿Eliminar ${pendingDeleteTx.type} · ${pendingDeleteTx.category} " +
                        "(${Money.format(pendingDeleteTx.amount)})? Se revertirá el saldo de la cuenta."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = pendingDeleteTx.id
                        pendingDeleteId = null
                        viewModel.deleteTransaction(id)
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
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
        item(key = "title") {
            SectionTitle(
                title = "Movimientos",
                subtitle = "Registra ingresos y gastos con precisión."
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
            Text("Historial", style = MaterialTheme.typography.titleMedium)
        }

        items(
            items = transactions,
            key = { it.id },
            contentType = { "tx" }
        ) { tx ->
            TransactionHistoryItem(
                tx = tx,
                isDeleting = form.deletingId == tx.id,
                onDeleteClick = { pendingDeleteId = tx.id }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var accountExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    val selectedAccount = remember(accounts, form.accountId) {
        accounts.firstOrNull { it.id == form.accountId } ?: accounts.firstOrNull()
    }

    SoftPanel {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TransactionType.all.forEach { type ->
                FilterChip(
                    selected = form.type == type,
                    onClick = { onTypeChange(type) },
                    label = { Text(type) }
                )
            }
        }

        ExposedDropdownMenuBox(
            expanded = accountExpanded,
            onExpandedChange = { accountExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedAccount?.let { "${it.name} · ${Money.format(it.balance)}" }
                    ?: "Sin cuentas",
                onValueChange = {},
                readOnly = true,
                label = { Text("Cuenta") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded)
                },
                modifier = Modifier
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = accountExpanded,
                onDismissRequest = { accountExpanded = false }
            ) {
                accounts.forEach { account ->
                    DropdownMenuItem(
                        text = {
                            Text("${account.name} · ${Money.format(account.balance)}")
                        },
                        onClick = {
                            onAccountSelected(account.id)
                            accountExpanded = false
                        }
                    )
                }
            }
        }

        MoneyAmountField(
            value = form.amountInput,
            onValueChange = onAmountChange,
            label = "Monto"
        )

        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = it }
        ) {
            OutlinedTextField(
                value = form.category,
                onValueChange = {},
                readOnly = true,
                label = { Text("Categoría") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                },
                modifier = Modifier
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            onCategoryChange(category)
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = form.note,
            onValueChange = onNoteChange,
            label = { Text("Nota (opcional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onSave,
            enabled = !form.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (form.isSaving) "Guardando…" else "Guardar movimiento")
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
                Text("${tx.type} · ${tx.category}", fontWeight = FontWeight.Medium)
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
                    contentDescription = "Eliminar movimiento",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
