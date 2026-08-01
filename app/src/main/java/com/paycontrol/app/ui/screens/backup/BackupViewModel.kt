package com.paycontrol.app.ui.screens.backup

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paycontrol.app.R
import com.paycontrol.app.data.backup.BackupManager
import com.paycontrol.app.domain.util.AppLog
import com.paycontrol.app.domain.util.BackupPasswordPolicy
import com.paycontrol.app.domain.util.UiErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupUiState(
    val isExporting: Boolean = false,
    val isRestoring: Boolean = false,
    val showExportPassword: Boolean = false,
    val showRestoreConfirm: Boolean = false,
    val showWeakRestorePasswordConfirm: Boolean = false,
    val pendingRestoreUri: Uri? = null,
    val pendingRestorePassword: String? = null,
    val weakPasswordReason: String? = null,
    val inventory: BackupManager.Inventory? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val app: Application,
    private val backupManager: BackupManager,
    private val uiErrorMapper: UiErrorMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _shareEvents = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val shareEvents: SharedFlow<Intent> = _shareEvents.asSharedFlow()

    fun requestExport() {
        if (_uiState.value.isExporting || _uiState.value.isRestoring) return
        viewModelScope.launch {
            val inventory = runCatching { backupManager.currentInventory() }
                .onFailure { AppLog.e(TAG, "Error al leer inventario de respaldo", it) }
                .getOrNull()
            _uiState.update {
                it.copy(
                    showExportPassword = true,
                    inventory = inventory,
                    errorMessage = null,
                    successMessage = null
                )
            }
        }
    }

    fun dismissExportPassword() {
        _uiState.update { it.copy(showExportPassword = false) }
    }

    fun exportBackup(password: String, confirmPassword: String) {
        if (_uiState.value.isExporting || _uiState.value.isRestoring) return
        BackupPasswordPolicy.validate(password)?.let { issue ->
            _uiState.update {
                it.copy(
                    errorMessage = BackupPasswordPolicy.issueMessage(issue, app.resources)
                )
            }
            return
        }
        if (password != confirmPassword) {
            _uiState.update {
                it.copy(errorMessage = app.getString(R.string.backup_passwords_mismatch))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showExportPassword = false,
                    isExporting = true,
                    errorMessage = null,
                    successMessage = null
                )
            }
            runCatching {
                val file = backupManager.export(password)
                buildShareIntent(file)
            }.onSuccess { intent ->
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        successMessage = app.getString(R.string.backup_export_ready)
                    )
                }
                _shareEvents.emit(intent)
            }.onFailure { error ->
                AppLog.e(TAG, "Error al exportar respaldo", error)
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = uiErrorMapper.map(
                            error,
                            app.getString(R.string.backup_export_failed)
                        )
                    )
                }
            }
        }
    }

    fun onRestoreFilePicked(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val inventory = runCatching { backupManager.currentInventory() }
                .onFailure { AppLog.e(TAG, "Error al leer inventario antes de restaurar", it) }
                .getOrNull()
            _uiState.update {
                it.copy(
                    pendingRestoreUri = uri,
                    showRestoreConfirm = true,
                    inventory = inventory,
                    errorMessage = null,
                    successMessage = null
                )
            }
        }
    }

    fun dismissRestoreConfirm() {
        _uiState.update {
            it.copy(
                showRestoreConfirm = false,
                pendingRestoreUri = null,
                pendingRestorePassword = null,
                showWeakRestorePasswordConfirm = false,
                weakPasswordReason = null
            )
        }
    }

    fun dismissWeakRestorePasswordConfirm() {
        _uiState.update {
            it.copy(
                showWeakRestorePasswordConfirm = false,
                pendingRestorePassword = null,
                weakPasswordReason = null,
                showRestoreConfirm = true
            )
        }
    }

    /**
     * Valida la contraseña de restauración. Si no cumple la política, pide
     * confirmación adicional pero **no** bloquea el descifrado.
     */
    fun confirmRestore(password: String) {
        val uri = _uiState.value.pendingRestoreUri ?: return
        if (_uiState.value.isRestoring || _uiState.value.isExporting) return
        val policyIssue = BackupPasswordPolicy.validate(password)
        if (policyIssue != null) {
            _uiState.update {
                it.copy(
                    showRestoreConfirm = false,
                    showWeakRestorePasswordConfirm = true,
                    pendingRestorePassword = password,
                    weakPasswordReason = BackupPasswordPolicy.issueMessage(
                        policyIssue,
                        app.resources
                    ),
                    errorMessage = null,
                    successMessage = null
                )
            }
            return
        }
        performRestore(uri, password)
    }

    fun proceedWeakPasswordRestore() {
        val uri = _uiState.value.pendingRestoreUri ?: return
        val password = _uiState.value.pendingRestorePassword ?: return
        if (_uiState.value.isRestoring || _uiState.value.isExporting) return
        _uiState.update {
            it.copy(
                showWeakRestorePasswordConfirm = false,
                pendingRestorePassword = null,
                weakPasswordReason = null
            )
        }
        performRestore(uri, password)
    }

    private fun performRestore(uri: Uri, password: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showRestoreConfirm = false,
                    showWeakRestorePasswordConfirm = false,
                    isRestoring = true,
                    errorMessage = null,
                    successMessage = null
                )
            }
            runCatching {
                app.contentResolver.openInputStream(uri)?.use { stream ->
                    backupManager.import(stream, password)
                } ?: throw IllegalArgumentException(
                    app.getString(R.string.backup_open_failed)
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isRestoring = false,
                        pendingRestoreUri = null,
                        pendingRestorePassword = null,
                        successMessage = app.getString(R.string.backup_restore_ok)
                    )
                }
            }.onFailure { error ->
                AppLog.e(TAG, "Error al restaurar respaldo", error)
                _uiState.update {
                    it.copy(
                        isRestoring = false,
                        pendingRestoreUri = null,
                        pendingRestorePassword = null,
                        errorMessage = uiErrorMapper.map(
                            error,
                            app.getString(R.string.backup_restore_failed)
                        )
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private fun buildShareIntent(file: java.io.File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            app,
            "${app.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, app.getString(R.string.backup_share_subject))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    companion object {
        private const val TAG = "BackupVM"
    }
}
