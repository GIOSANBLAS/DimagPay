package com.paycontrol.app.ui.screens.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paycontrol.app.R
import com.paycontrol.app.data.local.entity.AccountEntity
import com.paycontrol.app.data.local.entity.TransactionEntity
import com.paycontrol.app.domain.model.TransactionType
import com.paycontrol.app.domain.util.DateTimeUtils
import com.paycontrol.app.domain.util.Money
import com.paycontrol.app.ui.components.SectionTitle
import com.paycontrol.app.ui.components.SoftPanel
import com.paycontrol.app.ui.components.StatusMessage
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val pagingItems = viewModel.pagedTransactions.collectAsLazyPagingItems()
    val context = LocalContext.current
    val listLoading = pagingItems.loadState.refresh is LoadState.Loading

    LaunchedEffect(Unit) {
        viewModel.shareEvents.collect { intent ->
            context.startActivity(
                android.content.Intent.createChooser(
                    intent,
                    context.getString(R.string.report_share_chooser)
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reports_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::exportCsv,
                        enabled = !state.isExporting && state.resultCount > 0
                    ) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.report_export_csv)
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "title") {
                SectionTitle(
                    title = stringResource(R.string.reports_title),
                    subtitle = stringResource(R.string.reports_subtitle)
                )
            }

            item(key = "filters") {
                SoftPanel {
                    Text(
                        stringResource(R.string.report_period),
                        style = MaterialTheme.typography.titleMedium
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DatePresetChip(
                            label = stringResource(R.string.report_preset_today),
                            selected = state.datePreset == ReportDatePreset.TODAY,
                            onClick = { viewModel.onDatePreset(ReportDatePreset.TODAY) }
                        )
                        DatePresetChip(
                            label = stringResource(R.string.report_preset_7d),
                            selected = state.datePreset == ReportDatePreset.DAYS_7,
                            onClick = { viewModel.onDatePreset(ReportDatePreset.DAYS_7) }
                        )
                        DatePresetChip(
                            label = stringResource(R.string.report_preset_30d),
                            selected = state.datePreset == ReportDatePreset.DAYS_30,
                            onClick = { viewModel.onDatePreset(ReportDatePreset.DAYS_30) }
                        )
                        DatePresetChip(
                            label = stringResource(R.string.report_preset_all),
                            selected = state.datePreset == ReportDatePreset.ALL,
                            onClick = { viewModel.onDatePreset(ReportDatePreset.ALL) }
                        )
                    }

                    Text(
                        stringResource(R.string.report_type),
                        style = MaterialTheme.typography.titleMedium
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = state.typeFilter == null,
                            onClick = { viewModel.onTypeFilter(null) },
                            label = { Text(stringResource(R.string.type_all)) }
                        )
                        FilterChip(
                            selected = state.typeFilter == TransactionType.INCOME,
                            onClick = { viewModel.onTypeFilter(TransactionType.INCOME) },
                            label = { Text(stringResource(R.string.type_income)) }
                        )
                        FilterChip(
                            selected = state.typeFilter == TransactionType.EXPENSE,
                            onClick = { viewModel.onTypeFilter(TransactionType.EXPENSE) },
                            label = { Text(stringResource(R.string.type_expense)) }
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
                        label = { Text(stringResource(R.string.report_category_optional)) },
                        placeholder = { Text(stringResource(R.string.report_category_hint)) }
                    )
                }
            }

            item(key = "totals") {
                SoftPanel {
                    Text(
                        stringResource(R.string.report_totals),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TotalMetric(
                            stringResource(R.string.dashboard_income),
                            Money.format(state.incomeCents)
                        )
                        TotalMetric(
                            stringResource(R.string.dashboard_expense),
                            Money.format(state.expenseCents)
                        )
                        TotalMetric(
                            stringResource(R.string.report_net),
                            Money.format(state.netCents)
                        )
                    }
                    Text(
                        when {
                            state.isLoading || listLoading -> stringResource(R.string.report_updating)
                            else -> stringResource(R.string.report_result_count, state.resultCount)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = viewModel::exportCsv,
                        enabled = !state.isExporting && state.resultCount > 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (state.isExporting) {
                                stringResource(R.string.report_exporting)
                            } else {
                                stringResource(R.string.report_export_csv)
                            }
                        )
                    }
                    StatusMessage(state.errorMessage, state.successMessage)
                }
            }

            item(key = "chart") {
                IncomeExpenseBarChart(
                    incomeCents = state.incomeCents,
                    expenseCents = state.expenseCents
                )
            }

            item(key = "list_title") {
                Text(
                    stringResource(R.string.transactions_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (!listLoading && !state.isLoading &&
                pagingItems.itemCount == 0 && state.resultCount == 0
            ) {
                item(key = "empty") {
                    SoftPanel {
                        Text(
                            stringResource(R.string.report_empty_filter),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey { it.id },
                    contentType = { "tx" }
                ) { index ->
                    val tx = pagingItems[index]
                    if (tx != null) {
                        ReportTransactionRow(tx)
                    }
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

@Composable
private fun AccountFilterField(
    accounts: List<AccountEntity>,
    selectedAccountId: Long?,
    onAccountSelected: (Long?) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    LaunchedEffect(accounts, selectedAccountId) {
        if (selectedAccountId != null && accounts.none { it.id == selectedAccountId }) {
            onAccountSelected(null)
        }
    }
    val selected = remember(accounts, selectedAccountId) {
        accounts.firstOrNull { it.id == selectedAccountId }
    }
    val allAccountsLabel = stringResource(R.string.report_all_accounts)
    val label = selected?.let { "${it.name} · ${Money.format(it.balance)}" }
        ?: allAccountsLabel

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(stringResource(R.string.report_account)) },
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
            title = { Text(stringResource(R.string.report_account)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = allAccountsLabel,
                        fontWeight = if (selected == null) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected == null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAccountSelected(null)
                                showPicker = false
                            }
                            .padding(vertical = 12.dp)
                    )
                    accounts.forEach { account ->
                        val isSelected = account.id == selected?.id
                        Text(
                            text = "${account.name} · ${Money.format(account.balance)}",
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAccountSelected(account.id)
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

/** Simple two-bar chart (ingresos vs gastos) for the filtered period — Canvas only. */
@Composable
private fun IncomeExpenseBarChart(
    incomeCents: Long,
    expenseCents: Long
) {
    val incomeColor = MaterialTheme.colorScheme.primary
    val expenseColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val incomeLabel = Money.format(incomeCents)
    val expenseLabel = Money.format(expenseCents)

    SoftPanel {
        Text(
            stringResource(R.string.report_income_vs_expense),
            style = MaterialTheme.typography.titleMedium
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
        ) {
            val maxCents = max(max(incomeCents, expenseCents), 1L).toFloat()
            val gap = size.width * 0.12f
            val barWidth = (size.width - gap * 3f) / 2f
            val topPad = size.height * 0.08f
            val usableHeight = size.height - topPad

            fun barHeight(cents: Long): Float =
                (cents.toFloat() / maxCents) * usableHeight

            fun drawBar(index: Int, cents: Long, color: Color) {
                val h = barHeight(cents).coerceAtLeast(4.dp.toPx())
                val left = gap + index * (barWidth + gap)
                val top = size.height - h
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(left, topPad),
                    size = Size(barWidth, usableHeight),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
            }

            drawBar(0, incomeCents, incomeColor)
            drawBar(1, expenseCents, expenseColor)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.dashboard_income),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(incomeLabel, fontWeight = FontWeight.SemiBold, color = incomeColor)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.dashboard_expense),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(expenseLabel, fontWeight = FontWeight.SemiBold, color = expenseColor)
            }
        }
    }
}

@Composable
private fun ReportTransactionRow(tx: TransactionEntity) {
    val isIncome = tx.type == TransactionType.INCOME
    val typeLabel = when (tx.type) {
        TransactionType.INCOME -> stringResource(R.string.type_income)
        TransactionType.EXPENSE -> stringResource(R.string.type_expense)
        else -> tx.type
    }
    val dateLabel = remember(tx.date) { DateTimeUtils.formatDisplay(tx.date) }
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
                    "$dateLabel · $typeLabel",
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
