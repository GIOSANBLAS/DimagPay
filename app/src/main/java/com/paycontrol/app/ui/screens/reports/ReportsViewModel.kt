package com.paycontrol.app.ui.screens.reports

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paycontrol.app.data.local.entity.AccountEntity
import com.paycontrol.app.data.local.entity.TransactionEntity
import com.paycontrol.app.data.repository.FinanceRepository
import com.paycontrol.app.domain.model.TransactionType
import com.paycontrol.app.domain.util.Money
import com.paycontrol.app.domain.util.UiErrorMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ReportDatePreset {
    TODAY,
    DAYS_7,
    DAYS_30,
    ALL
}

data class ReportsUiState(
    val datePreset: ReportDatePreset = ReportDatePreset.DAYS_30,
    val fromMs: Long? = null,
    val toMs: Long? = null,
    val accountId: Long? = null,
    val typeFilter: String? = null,
    val categoryQuery: String = "",
    val transactions: List<TransactionEntity> = emptyList(),
    val incomeCents: Long = 0L,
    val expenseCents: Long = 0L,
    val netCents: Long = 0L,
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ReportsViewModel(
    private val app: Application,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    val accounts: StateFlow<List<AccountEntity>> = financeRepository
        .observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(
        ReportsUiState().let { initial ->
            val (from, to) = resolveRange(initial.datePreset)
            initial.copy(fromMs = from, toMs = to)
        }
    )
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    private val _shareEvents = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val shareEvents: SharedFlow<Intent> = _shareEvents.asSharedFlow()

    private var refreshJob: Job? = null
    private var categoryJob: Job? = null

    init {
        refresh()
    }

    fun onDatePreset(preset: ReportDatePreset) {
        val (from, to) = resolveRange(preset)
        _uiState.update {
            it.copy(
                datePreset = preset,
                fromMs = from,
                toMs = to,
                errorMessage = null,
                successMessage = null
            )
        }
        refresh()
    }

    fun onAccountFilter(accountId: Long?) {
        _uiState.update {
            it.copy(accountId = accountId, errorMessage = null, successMessage = null)
        }
        refresh()
    }

    fun onTypeFilter(type: String?) {
        _uiState.update {
            it.copy(typeFilter = type, errorMessage = null, successMessage = null)
        }
        refresh()
    }

    fun onCategoryQuery(value: String) {
        _uiState.update {
            it.copy(categoryQuery = value.take(80), errorMessage = null, successMessage = null)
        }
        categoryJob?.cancel()
        categoryJob = viewModelScope.launch {
            delay(300)
            refresh()
        }
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val current = _uiState.value
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val list = financeRepository.getTransactionsFiltered(
                    fromMs = current.fromMs,
                    toMs = current.toMs,
                    accountId = current.accountId,
                    type = current.typeFilter,
                    category = current.categoryQuery
                )
                val income = list
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amount }
                val expense = list
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }
                _uiState.update {
                    it.copy(
                        transactions = list,
                        incomeCents = income,
                        expenseCents = expense,
                        netCents = Money.subtract(income, expense),
                        isLoading = false
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = UiErrorMapper.map(error, "No se pudo cargar el reporte")
                    )
                }
            }
        }
    }

    fun exportCsv() {
        val current = _uiState.value
        if (current.transactions.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "No hay movimientos para exportar")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(isExporting = true, errorMessage = null, successMessage = null)
            }
            runCatching {
                writeCsvAndBuildShareIntent(current.transactions)
            }.onSuccess { intent ->
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        successMessage = "CSV listo para compartir"
                    )
                }
                _shareEvents.emit(intent)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = UiErrorMapper.map(error, "No se pudo exportar el CSV")
                    )
                }
            }
        }
    }

    private fun writeCsvAndBuildShareIntent(transactions: List<TransactionEntity>): Intent {
        val dir = File(app.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "paycontrol_reporte_$stamp.csv")
        val dateFmt = SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.Builder().setLanguage("es").setRegion("MX").build()
        )
        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine("id,fecha,tipo,categoria,monto_centavos,monto,cuenta_id,nota")
            transactions.forEach { tx ->
                writer.appendLine(
                    listOf(
                        tx.id.toString(),
                        csvEscape(dateFmt.format(Date(tx.date))),
                        tx.type,
                        csvEscape(tx.category),
                        tx.amount.toString(),
                        csvEscape(Money.format(tx.amount)),
                        tx.accountId.toString(),
                        csvEscape(tx.note)
                    ).joinToString(",")
                )
            }
        }
        val uri: Uri = FileProvider.getUriForFile(
            app,
            "${app.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Reporte PayControl")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    companion object {
        fun resolveRange(preset: ReportDatePreset): Pair<Long?, Long?> {
            val now = System.currentTimeMillis()
            return when (preset) {
                ReportDatePreset.TODAY -> {
                    val start = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val end = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    start to end
                }

                ReportDatePreset.DAYS_7 -> {
                    (now - 7L * 24 * 60 * 60 * 1000) to now
                }

                ReportDatePreset.DAYS_30 -> {
                    (now - 30L * 24 * 60 * 60 * 1000) to now
                }

                ReportDatePreset.ALL -> null to null
            }
        }
    }
}
