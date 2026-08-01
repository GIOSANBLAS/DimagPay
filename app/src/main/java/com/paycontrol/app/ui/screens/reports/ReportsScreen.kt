package com.paycontrol.app.ui.screens.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paycontrol.app.data.local.entity.AccountEntity
import com.paycontrol.app.data.local.entity.TransactionEntity
import com.paycontrol.app.domain.model.TransactionType
import com.paycontrol.app.domain.util.Money
import com.paycontrol.app.ui.components.SectionTitle
import com.paycontrol.app.ui.components.SoftPanel
import com.paycontrol.app.ui.components.StatusMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.shareEvents.collect { intent ->
            context.startActivity(
                android.content.Intent.createChooser(intent, "Compartir reporte CSV")
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::exportCsv,
                        enabled = !state.isExporting && state.transactions.isNotEmpty()
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = "Exportar CSV")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "title") {
                SectionTitle(
                    title = "Reportes",
                    subtitle = "Filtra movimientos, consulta totales y exporta un CSV."
                )
            }

            item(key = "filters") {
                SoftPanel {
                    Text("Periodo", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DatePresetChip(
                            label = "Hoy",
                            selected = state.datePreset == ReportDatePreset.TODAY,
                            onClick = { viewModel.onDatePreset(ReportDatePreset.TODAY) }
                        )
                        DatePresetChip(
                            label = "7 días",
                            selected = state.datePreset == ReportDatePreset.DAYS_7,
                            onClick = { viewModel.onDatePreset(ReportDatePreset.DAYS_7) }
                        )
                        DatePresetChip(
                            label = "30 días",
                            selected = state.datePreset == ReportDatePreset.DAYS_30,
                            onClick = { viewModel.onDatePreset(ReportDatePreset.DAYS_30) }
                        )
                        DatePresetChip(
                            label = "Todo",
                            selected = state.datePreset == ReportDatePreset.ALL,
                            onClick = { viewModel.onDatePreset(ReportDatePreset.ALL) }
                        )
                    }

                    Text("Tipo", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = state.typeFilter == null,
                            onClick = { viewModel.onTypeFilter(null) },
                            label = { Text("Todos") }
                        )
                        FilterChip(
                            selected = state.typeFilter == TransactionType.INCOME,
                            onClick = { viewModel.onTypeFilter(TransactionType.INCOME) },
                            label = { Text(TransactionType.INCOME) }
                        )
                        FilterChip(
                            selected = state.typeFilter == TransactionType.EXPENSE,
                            onClick = { viewModel.onTypeFilter(TransactionType.EXPENSE) },
                            label = { Text(TransactionType.EXPENSE) }
                        )
                    }

                    AccountFilterField(
                        accounts = accounts,
                        selectedAccountId = state.accountId,
                        onAccountSelected = viewModel::onAccountFilter
                    )

                    OutlinedTextField(
                        value = state.categoryQuery,
                        onValueChange = viewModel::onCategoryQuery,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Categoría (opcional)") },
                        placeholder = { Text("Ej. Ventas, Nómina…") }
                    )
                }
            }

            item(key = "totals") {
                SoftPanel {
                    Text("Totales del filtro", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TotalMetric("Ingresos", Money.format(state.incomeCents))
                        TotalMetric("Gastos", Money.format(state.expenseCents))
                        TotalMetric("Neto", Money.format(state.netCents))
                    }
                    Text(
                        if (state.isLoading) {
                            "Actualizando…"
                        } else {
                            "${state.transactions.size} movimiento(s)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = viewModel::exportCsv,
                        enabled = !state.isExporting && state.transactions.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (state.isExporting) "Exportando…" else "Exportar CSV"
                        )
                    }
                    StatusMessage(state.errorMessage, state.successMessage)
                }
            }

            item(key = "list_title") {
                Text("Movimientos", style = MaterialTheme.typography.titleMedium)
            }

            if (!state.isLoading && state.transactions.isEmpty()) {
                item(key = "empty") {
                    SoftPanel {
                        Text(
                            "No hay movimientos con estos filtros.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    items = state.transactions,
                    key = { it.id },
                    contentType = { "tx" }
                ) { tx ->
                    ReportTransactionRow(tx)
                }
            }
        }
    }
}

@Composable
private fun DatePresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountFilterField(
    accounts: List<AccountEntity>,
    selectedAccountId: Long?,
    onAccountSelected: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = remember(accounts, selectedAccountId) {
        accounts.firstOrNull { it.id == selectedAccountId }
    }
    val label = selected?.let { "${it.name} · ${Money.format(it.balance)}" }
        ?: "Todas las cuentas"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Cuenta") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Todas las cuentas") },
                onClick = {
                    onAccountSelected(null)
                    expanded = false
                }
            )
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = {
                        Text("${account.name} · ${Money.format(account.balance)}")
                    },
                    onClick = {
                        onAccountSelected(account.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TotalMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ReportTransactionRow(tx: TransactionEntity) {
    val isIncome = tx.type == TransactionType.INCOME
    val dateLabel = remember(tx.date) {
        SimpleDateFormat(
            "dd MMM yyyy",
            Locale.Builder().setLanguage("es").setRegion("MX").build()
        ).format(Date(tx.date))
    }
    val amountLabel = remember(tx.amount, isIncome) {
        (if (isIncome) "+" else "−") + Money.format(tx.amount)
    }
    SoftPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.category, fontWeight = FontWeight.Medium)
                Text(
                    "$dateLabel · ${tx.type}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (tx.note.isNotBlank()) {
                    Text(
                        tx.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = amountLabel,
                color = if (isIncome) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
