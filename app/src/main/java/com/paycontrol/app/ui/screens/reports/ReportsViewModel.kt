package com.paycontrol.app.ui.screens.reports

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.paycontrol.app.R
import com.paycontrol.app.data.local.entity.AccountEntity
import com.paycontrol.app.data.local.entity.TransactionEntity
import com.paycontrol.app.data.repository.FinanceRepository
import com.paycontrol.app.domain.model.TransactionType
import com.paycontrol.app.domain.util.AppLog
import com.paycontrol.app.domain.util.DateTimeUtils
import com.paycontrol.app.domain.util.Money
import com.paycontrol.app.domain.util.UiErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val resultCount: Int = 0,
    val incomeCents: Long = 0L,
    val expenseCents: Long = 0L,
    val netCents: Long = 0L,
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

private data class ReportFilter(
    val fromMs: Long?,
    val toMs: Long?,
    val accountId: Long?,
    val typeFilter: String?,
    val categoryQuery: String
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val app: Application,
    private val financeRepository: FinanceRepository,
    private val uiErrorMapper: UiErrorMapper
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

    private val filterFlow: Flow<ReportFilter> = _uiState
        .map {
            ReportFilter(
                fromMs = it.fromMs,
                toMs = it.toMs,
                accountId = it.accountId,
                typeFilter = it.typeFilter,
                categoryQuery = it.categoryQuery.trim()
            )
        }
        .distinctUntilChanged()

    val pagedTransactions: Flow<PagingData<TransactionEntity>> = filterFlow
        .flatMapLatest { filter ->
            Pager(
                config = PagingConfig(pageSize = 40, enablePlaceholders = false)
            ) {
                financeRepository.filteredTransactionsPagingSource(
                    fromMs = filter.fromMs,
                    toMs = filter.toMs,
                    accountId = filter.accountId,
                    type = filter.typeFilter,
                    category = filter.categoryQuery
                )
            }.flow
        }
        .cachedIn(viewModelScope)

    private val _shareEvents = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val shareEvents: SharedFlow<Intent> = _shareEvents.asSharedFlow()

    private var totalsJob: Job? = null

    init {
        viewModelScope.launch {
            filterFlow.debounce(300).collect { refreshTotals() }
        }
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
    }

    fun onAccountFilter(accountId: Long?) {
        _uiState.update {
            it.copy(accountId = accountId, errorMessage = null, successMessage = null)
        }
    }

    fun onTypeFilter(type: String?) {
        _uiState.update {
            it.copy(typeFilter = type, errorMessage = null, successMessage = null)
        }
    }

    fun onCategoryQuery(value: String) {
        _uiState.update {
            it.copy(categoryQuery = value.take(80), errorMessage = null, successMessage = null)
        }
    }

    fun refreshTotals() {
        totalsJob?.cancel()
        totalsJob = viewModelScope.launch {
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
                        resultCount = list.size,
                        incomeCents = income,
                        expenseCents = expense,
                        netCents = Money.subtract(income, expense),
                        isLoading = false
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                AppLog.e(TAG, "Error al calcular totales del reporte", error)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = uiErrorMapper.map(
                            error,
                            app.getString(R.string.error_report_load)
                        )
                    )
                }
            }
        }
    }

    fun exportCsv() {
        if (_uiState.value.resultCount <= 0) {
            _uiState.update {
                it.copy(errorMessage = app.getString(R.string.error_report_empty_export))
            }
            return
        }
        viewModelScope.launch {
            val current = _uiState.value
            _uiState.update {
                it.copy(isExporting = true, errorMessage = null, successMessage = null)
            }
            runCatching {
                val list = financeRepository.getTransactionsFiltered(
                    fromMs = current.fromMs,
                    toMs = current.toMs,
                    accountId = current.accountId,
                    type = current.typeFilter,
                    category = current.categoryQuery
                )
                if (list.isEmpty()) {
                    error(app.getString(R.string.error_report_empty_export))
                }
                writeCsvAndBuildShareIntent(list)
            }.onSuccess { intent ->
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        successMessage = app.getString(R.string.report_csv_ready)
                    )
                }
                _shareEvents.emit(intent)
            }.onFailure { error ->
                AppLog.e(TAG, "Error al exportar CSV", error)
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = uiErrorMapper.map(
                            error,
                            app.getString(R.string.error_report_export)
                        )
                    )
                }
            }
        }
    }

    private fun writeCsvAndBuildShareIntent(transactions: List<TransactionEntity>): Intent {
        val dir = File(app.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "dimagpay_reporte_$stamp.csv")
        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine("id,fecha,tipo,categoria,monto_centavos,monto,cuenta_id,nota")
            transactions.forEach { tx ->
                writer.appendLine(
                    listOf(
                        tx.id.toString(),
                        csvEscape(DateTimeUtils.formatNumeric(tx.date)),
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
            putExtra(Intent.EXTRA_SUBJECT, app.getString(R.string.report_csv_subject))
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
        private const val TAG = "ReportsVM"

        fun resolveRange(preset: ReportDatePreset): Pair<Long?, Long?> {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            return when (preset) {
                ReportDatePreset.TODAY -> {
                    val start = DateTimeUtils.startOfLocalDayMillis(today, zone)
                    start to DateTimeUtils.endOfLocalDayMillis(start, zone)
                }

                ReportDatePreset.DAYS_7 -> {
                    val start = DateTimeUtils.startOfLocalDayMillis(today.minusDays(6), zone)
                    val endStart = DateTimeUtils.startOfLocalDayMillis(today, zone)
                    start to DateTimeUtils.endOfLocalDayMillis(endStart, zone)
                }

                ReportDatePreset.DAYS_30 -> {
                    val start = DateTimeUtils.startOfLocalDayMillis(today.minusDays(29), zone)
                    val endStart = DateTimeUtils.startOfLocalDayMillis(today, zone)
                    start to DateTimeUtils.endOfLocalDayMillis(endStart, zone)
                }

                ReportDatePreset.ALL -> null to null
            }
        }
    }
}
