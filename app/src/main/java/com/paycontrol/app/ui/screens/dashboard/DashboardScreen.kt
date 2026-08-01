package com.paycontrol.app.ui.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paycontrol.app.data.local.entity.TransactionEntity
import com.paycontrol.app.domain.model.TransactionType
import com.paycontrol.app.domain.util.Money
import com.paycontrol.app.ui.components.SoftPanel
import com.paycontrol.app.ui.navigation.AppDestination
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigate: (AppDestination) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val greeting = remember(state.displayName) {
        if (state.displayName.isBlank()) "Hola" else "Hola, ${state.displayName}"
    }
    val balanceFormatted = remember(state.consolidatedBalanceCents) {
        Money.format(state.consolidatedBalanceCents)
    }
    val incomeFormatted = remember(state.totalIncomeCents) {
        Money.format(state.totalIncomeCents)
    }
    val expenseFormatted = remember(state.totalExpenseCents) {
        Money.format(state.totalExpenseCents)
    }
    val accountsFormatted = remember(state.accountsBalanceCents) {
        Money.format(state.accountsBalanceCents)
    }
    val receivablesFormatted = remember(state.receivablesCents) {
        Money.format(state.receivablesCents)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(greeting, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Resumen financiero",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onNavigate(AppDestination.Settings) }) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Ajustes")
                }
            }
        }

        item(key = "balance_card") {
            BalanceSummaryCard(
                balance = balanceFormatted,
                income = incomeFormatted,
                expense = expenseFormatted
            )
        }

        item(key = "shortcuts") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Shortcut(
                        title = "Movimientos",
                        icon = Icons.Outlined.SwapHoriz,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppDestination.Transactions) }
                    )
                    Shortcut(
                        title = "Reportes",
                        icon = Icons.Outlined.Assessment,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppDestination.Reports) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Shortcut(
                        title = "Proveedores",
                        icon = Icons.Outlined.LocalShipping,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppDestination.Suppliers) }
                    )
                    Shortcut(
                        title = "Clientes",
                        icon = Icons.Outlined.Groups,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppDestination.Clients) }
                    )
                    Shortcut(
                        title = "Cuentas",
                        icon = Icons.Outlined.AccountBalanceWallet,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppDestination.Accounts) }
                    )
                }
            }
        }

        item(key = "accounts_card") {
            SoftPanel(
                modifier = Modifier.clickable { onNavigate(AppDestination.Accounts) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Saldo en cuentas", style = MaterialTheme.typography.titleMedium)
                    Text(accountsFormatted, fontWeight = FontWeight.Medium)
                }
                Text(
                    "Por cobrar: $receivablesFormatted",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    "Toca para administrar cuentas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(key = "recent_title") {
            Text("Últimas transacciones", style = MaterialTheme.typography.titleMedium)
        }

        if (state.recentTransactions.isEmpty()) {
            item(key = "recent_empty") {
                SoftPanel {
                    Text(
                        "Aún no hay movimientos. Empieza en la pestaña Movimientos.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(
                items = state.recentTransactions,
                key = { it.id },
                contentType = { "tx" }
            ) { tx ->
                TransactionRow(tx)
            }
        }
    }
}

@Composable
private fun BalanceSummaryCard(
    balance: String,
    income: String,
    expense: String
) {
    SoftPanel {
        Text(
            "Balance consolidado",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = balance,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Metric("Ingresos", income)
            Metric("Gastos", expense)
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Shortcut(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun TransactionRow(tx: TransactionEntity) {
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
