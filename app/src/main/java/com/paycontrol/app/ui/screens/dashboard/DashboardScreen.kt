package com.paycontrol.app.ui.screens.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paycontrol.app.R
import com.paycontrol.app.data.local.entity.TransactionEntity
import com.paycontrol.app.domain.model.TransactionType
import com.paycontrol.app.domain.util.DateTimeUtils
import com.paycontrol.app.domain.util.Money
import com.paycontrol.app.ui.components.BrandWordmark
import com.paycontrol.app.ui.components.HeroPanel
import com.paycontrol.app.ui.components.SoftPanel
import com.paycontrol.app.ui.navigation.AppDestination

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigate: (AppDestination) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val greeting = if (state.displayName.isBlank()) {
        stringResource(R.string.dashboard_greeting_fallback)
    } else {
        stringResource(R.string.dashboard_greeting_named, state.displayName)
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
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .alpha(appear.value),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    BrandWordmark()
                    Text(
                        greeting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onNavigate(AppDestination.Settings) }) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings)
                    )
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
                        title = stringResource(R.string.nav_transactions),
                        icon = Icons.Outlined.SwapHoriz,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppDestination.Transactions) }
                    )
                    Shortcut(
                        title = stringResource(R.string.reports_title),
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
                        title = stringResource(R.string.nav_suppliers),
                        icon = Icons.Outlined.LocalShipping,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppDestination.Suppliers) }
                    )
                    Shortcut(
                        title = stringResource(R.string.nav_clients),
                        icon = Icons.Outlined.Groups,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppDestination.Clients) }
                    )
                    Shortcut(
                        title = stringResource(R.string.accounts_title),
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
                    Text(
                        stringResource(R.string.dashboard_balance_accounts),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(accountsFormatted, fontWeight = FontWeight.Medium)
                }
                Text(
                    stringResource(R.string.dashboard_receivables, receivablesFormatted),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    stringResource(R.string.dashboard_tap_manage_accounts),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(key = "recent_title") {
            Text(
                stringResource(R.string.dashboard_recent),
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (state.recentTransactions.isEmpty()) {
            item(key = "recent_empty") {
                SoftPanel {
                    Text(
                        stringResource(R.string.dashboard_empty_tx),
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
    HeroPanel {
        Text(
            stringResource(R.string.dashboard_balance_accounts),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.78f)
        )
        Text(
            text = balance,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Metric(stringResource(R.string.dashboard_income), income, Color.White)
            Metric(stringResource(R.string.dashboard_expense), expense, Color.White.copy(alpha = 0.92f))
        }
    }
}

@Composable
private fun Metric(label: String, value: String, color: Color) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = color.copy(alpha = 0.75f)
        )
        Text(value, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun Shortcut(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick),
        shape = shape,
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
    val isTransfer = tx.type == TransactionType.TRANSFER
    val isIncome = tx.type == TransactionType.INCOME
    val typeLabel = when (tx.type) {
        TransactionType.INCOME -> stringResource(R.string.type_income)
        TransactionType.EXPENSE -> stringResource(R.string.type_expense)
        else -> tx.type
    }
    val dateLabel = remember(tx.date) {
        DateTimeUtils.formatDisplay(tx.date)
    }
    val amountLabel = remember(tx.amount, isIncome, isTransfer) {
        when {
            isTransfer -> "↔ ${Money.format(tx.amount)}"
            isIncome -> "+${Money.format(tx.amount)}"
            else -> "−${Money.format(tx.amount)}"
        }
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
            }
            Text(
                text = amountLabel,
                color = when {
                    isTransfer -> MaterialTheme.colorScheme.onSurfaceVariant
                    isIncome -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                },
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
