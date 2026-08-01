package com.paycontrol.app.ui.components

import android.Manifest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.paycontrol.app.data.contacts.ContactsRepository
import com.paycontrol.app.domain.util.AppLog
import com.paycontrol.app.data.contacts.DeviceContact
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ContactPickerSheet(
    contactsRepository: ContactsRepository,
    onDismiss: () -> Unit,
    onContactSelected: (DeviceContact) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val permission = rememberPermissionState(Manifest.permission.READ_CONTACTS)
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(permission.status.isGranted) {
        if (!permission.status.isGranted) {
            contacts = emptyList()
        }
    }

    fun load(q: String) {
        searchJob?.cancel()
        if (q.isBlank()) {
            contacts = emptyList()
            loading = false
            error = null
            return
        }
        searchJob = scope.launch {
            loading = true
            error = null
            delay(250)
            runCatching { contactsRepository.searchContacts(q, limit = 80) }
                .onSuccess { contacts = it }
                .onFailure { throwable ->
                    AppLog.e("ContactPicker", "Error al buscar contactos", throwable)
                    error = "No se pudieron leer los contactos"
                    contacts = emptyList()
                }
            loading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Outlined.Contacts, contentDescription = null)
                Text(
                    "Importar contacto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "Incluye contactos locales y los sincronizados con Google en este dispositivo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when {
                !permission.status.isGranted -> {
                    Text(
                        if (permission.status.shouldShowRationale) {
                            "Necesitamos permiso para mostrar tu agenda. No se sube a ningún servidor."
                        } else {
                            "Autoriza el acceso a contactos para importar más rápido."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = { permission.launchPermissionRequest() }) {
                        Text("Permitir contactos")
                    }
                }

                else -> {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            load(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Buscar contacto") },
                        placeholder = { Text("Escribe nombre o teléfono") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) }
                    )

                    if (query.isBlank() && !loading) {
                        Text(
                            "Escribe para buscar en tu agenda (local o Google sincronizada).",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }

                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!loading && query.isNotBlank() && contacts.isEmpty()) {
                            item {
                                Text(
                                    "No hay contactos con teléfono coincidentes.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        items(contacts, key = { it.id }) { contact ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onContactSelected(contact)
                                    }
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(contact.name, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "${contact.phone} · ${contact.sourceLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
