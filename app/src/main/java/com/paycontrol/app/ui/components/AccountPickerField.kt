package com.paycontrol.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paycontrol.app.data.local.entity.AccountEntity
import com.paycontrol.app.domain.util.Money

/**
 * Selector de cuenta seguro dentro de LazyColumn.
 * Evita ExposedDropdownMenu (suele crashear al expandirse en listas scrollables).
 */
@Composable
fun AccountPickerField(
    accounts: List<AccountEntity>,
    selectedAccountId: Long?,
    onAccountSelected: (Long) -> Unit,
    label: String = "Cuenta"
) {
    var showPicker by remember { mutableStateOf(false) }
    val selected = remember(accounts, selectedAccountId) {
        accounts.firstOrNull { it.id == selectedAccountId } ?: accounts.firstOrNull()
    }

    LaunchedEffect(accounts, selectedAccountId) {
        when {
            accounts.isEmpty() -> Unit
            selectedAccountId == null || accounts.none { it.id == selectedAccountId } -> {
                accounts.firstOrNull()?.id?.let(onAccountSelected)
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected?.let { "${it.name} · ${Money.format(it.balance)}" }
                ?: "Sin cuentas disponibles",
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
        if (accounts.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showPicker = true }
            )
        }
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
fun StatusMessage(error: String?, success: String?) {
    error?.let {
        Text(it, color = MaterialTheme.colorScheme.error)
    }
    success?.let {
        Text(it, color = MaterialTheme.colorScheme.primary)
    }
}
