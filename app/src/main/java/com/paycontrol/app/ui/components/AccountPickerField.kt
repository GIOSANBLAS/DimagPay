package com.paycontrol.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.paycontrol.app.data.local.entity.AccountEntity
import com.paycontrol.app.domain.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerField(
    accounts: List<AccountEntity>,
    selectedAccountId: Long?,
    onAccountSelected: (Long) -> Unit,
    label: String = "Cuenta"
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = remember(accounts, selectedAccountId) {
        accounts.firstOrNull { it.id == selectedAccountId } ?: accounts.firstOrNull()
    }

    // Solo auto-selecciona cuando aún no hay cuenta elegida (evita bucles de recomposición).
    LaunchedEffect(accounts, selectedAccountId) {
        if (selectedAccountId == null) {
            accounts.firstOrNull()?.id?.let(onAccountSelected)
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it && accounts.isNotEmpty() }
    ) {
        OutlinedTextField(
            value = selected?.let { "${it.name} · ${Money.format(it.balance)}" }
                ?: "Sin cuentas disponibles",
            onValueChange = {},
            readOnly = true,
            enabled = accounts.isNotEmpty(),
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text("${account.name} · ${Money.format(account.balance)}") },
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
fun StatusMessage(error: String?, success: String?) {
    error?.let {
        Text(it, color = MaterialTheme.colorScheme.error)
    }
    success?.let {
        Text(it, color = MaterialTheme.colorScheme.primary)
    }
}
