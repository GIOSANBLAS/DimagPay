package com.paycontrol.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.paycontrol.app.data.local.AppDatabase
import com.paycontrol.app.data.local.dao.AccountDao
import com.paycontrol.app.data.local.dao.ClientDao
import com.paycontrol.app.data.local.dao.LedgerDao
import com.paycontrol.app.data.local.dao.SupplierDao
import com.paycontrol.app.data.local.dao.TransactionDao
import com.paycontrol.app.data.local.entity.AccountEntity
import com.paycontrol.app.data.local.entity.TransactionEntity
import com.paycontrol.app.domain.model.AccountType
import com.paycontrol.app.domain.model.DefaultCategories
import com.paycontrol.app.domain.model.TransactionType
import com.paycontrol.app.domain.util.AppLog
import com.paycontrol.app.domain.util.DateTimeUtils
import com.paycontrol.app.domain.util.Money
import com.paycontrol.app.ui.widget.BalanceWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Punto de verdad financiero.
 * Mutaciones multi-tabla: RoomDatabase.withTransaction (atómicas entre DAOs).
 * Posteo movimiento+saldo: LedgerDao.@Transaction.
 * Todo en Dispatchers.IO.
 */
class FinanceRepository(
    context: Context,
    private val database: AppDatabase,
    private val accountDao: AccountDao = database.accountDao(),
    private val transactionDao: TransactionDao = database.transactionDao(),
    private val clientDao: ClientDao = database.clientDao(),
    private val supplierDao: SupplierDao = database.supplierDao(),
    private val ledgerDao: LedgerDao = database.ledgerDao()
) {
    private val appContext = context.applicationContext

    private fun notifyWidgets() {
        BalanceWidgetProvider.requestUpdate(appContext)
    }

    fun observeAccounts(): Flow<List<AccountEntity>> =
        accountDao.observeAll()
            .catch { e ->
                AppLog.e(TAG, "Error observing accounts", e)
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)

    fun observeRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>> =
        transactionDao.observeRecent(limit.coerceIn(1, 100))
            .catch { e ->
                AppLog.e(TAG, "Error observing recent transactions", e)
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)

    fun observeAllTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.observeAll()
            .catch { e ->
                AppLog.e(TAG, "Error observing all transactions", e)
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)

    fun transactionsPagingSource(): androidx.paging.PagingSource<Int, TransactionEntity> =
        transactionDao.pagingSource()

    fun filteredTransactionsPagingSource(
        fromMs: Long?,
        toMs: Long?,
        accountId: Long?,
        type: String?,
        category: String?
    ): androidx.paging.PagingSource<Int, TransactionEntity> =
        transactionDao.pagingFiltered(
            fromMs = fromMs,
            toMs = toMs,
            accountId = accountId,
            type = type?.takeIf { it.isNotBlank() },
            category = category?.trim()?.takeIf { it.isNotEmpty() }
        )

    fun observeTotalIncome(): Flow<Long> =
        transactionDao.observeTotalByType(TransactionType.INCOME)
            .catch { e ->
                AppLog.e(TAG, "Error observing total income", e)
                emit(0L)
            }
            .flowOn(Dispatchers.IO)

    fun observeTotalExpense(): Flow<Long> =
        transactionDao.observeTotalByType(TransactionType.EXPENSE)
            .catch { e ->
                AppLog.e(TAG, "Error observing total expense", e)
                emit(0L)
            }
            .flowOn(Dispatchers.IO)

    fun observeConsolidatedBalance(): Flow<Long> =
        combine(observeTotalIncome(), observeTotalExpense()) { income, expense ->
            Money.subtract(income, expense)
        }.catch { e ->
            AppLog.e(TAG, "Error observing consolidated balance", e)
            emit(0L)
        }

    fun observeAccountsBalance(): Flow<Long> =
        accountDao.observeTotalBalance()
            .catch { e ->
                AppLog.e(TAG, "Error observing accounts balance", e)
                emit(0L)
            }
            .flowOn(Dispatchers.IO)

    suspend fun getAccountsBalance(): Long = withContext(Dispatchers.IO) {
        accountDao.getTotalBalance()
    }

    suspend fun getTransactionsFiltered(
        fromMs: Long?,
        toMs: Long?,
        accountId: Long? = null,
        type: String? = null,
        category: String? = null
    ): List<TransactionEntity> = withContext(Dispatchers.IO) {
        val trimmedCategory = category?.trim()?.takeIf { it.isNotEmpty() }
        transactionDao.getFiltered(
            fromMs = fromMs,
            toMs = toMs,
            accountId = accountId,
            type = type?.takeIf { it.isNotBlank() },
            category = trimmedCategory
        )
    }

    suspend fun ensureDefaultAccount(): Long = withContext(Dispatchers.IO) {
        val existing = accountDao.getAll()
        if (existing.isNotEmpty()) return@withContext existing.first().id
        createAccountInternal(
            name = "Efectivo principal",
            type = AccountType.CASH,
            initialBalanceCents = 0L
        )
    }

    suspend fun createAccount(
        name: String,
        type: String,
        initialBalanceCents: Long = 0L
    ): Long = withContext(Dispatchers.IO) {
        createAccountInternal(name, type, initialBalanceCents).also { notifyWidgets() }
    }

    suspend fun updateAccount(
        id: Long,
        name: String,
        type: String
    ) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val existing = accountDao.getById(id)
                ?: error("Cuenta no encontrada")
            val trimmed = name.trim()
            require(trimmed.isNotBlank()) { "El nombre de la cuenta es obligatorio" }
            require(trimmed.length <= 80) { "El nombre de la cuenta es demasiado largo" }
            require(type in AccountType.all) { "Tipo de cuenta inválido" }
            accountDao.update(
                existing.copy(name = trimmed, type = type)
            )
        }
        notifyWidgets()
    }

    suspend fun deleteAccount(id: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val existing = accountDao.getById(id)
                ?: error("Cuenta no encontrada")
            val txCount = transactionDao.countByAccount(id)
            if (txCount > 0) {
                error(
                    "No se puede eliminar «${existing.name}»: tiene $txCount " +
                        if (txCount == 1) "movimiento registrado" else "movimientos registrados"
                )
            }
            accountDao.delete(existing)
        }
        notifyWidgets()
    }

    suspend fun registerTransaction(
        accountId: Long,
        amountCents: Long,
        type: String,
        category: String,
        date: Long,
        note: String = ""
    ): Long = withContext(Dispatchers.IO) {
        database.withTransaction {
            applyTransactionLocked(
                accountId = accountId,
                amountCents = amountCents,
                type = type,
                category = category,
                date = date,
                note = note
            )
        }.also { notifyWidgets() }
    }

    /** Abono cliente + INGRESO en cuenta (atómico). */
    suspend fun receiveClientPayment(
        clientId: Long,
        accountId: Long,
        amountCents: Long
    ): Long = withContext(Dispatchers.IO) {
        require(amountCents > 0L) { "El abono debe ser mayor a cero" }
        database.withTransaction {
            val client = clientDao.getById(clientId)
                ?: error("Cliente no encontrado")
            require(amountCents <= client.totalDebt) {
                "El abono supera la deuda pendiente (${Money.format(client.totalDebt)})"
            }
            val txId = applyTransactionLocked(
                accountId = accountId,
                amountCents = amountCents,
                type = TransactionType.INCOME,
                category = DefaultCategories.CLIENT_PAYMENT,
                date = DateTimeUtils.nowEpochMillis(),
                note = "Abono · ${client.name}",
                relatedClientId = clientId
            )
            clientDao.update(
                client.copy(totalDebt = Money.subtract(client.totalDebt, amountCents))
            )
            txId
        }.also { notifyWidgets() }
    }

    /** Pago proveedor + GASTO en cuenta (atómico). */
    suspend fun paySupplier(
        supplierId: Long,
        accountId: Long,
        amountCents: Long
    ): Long = withContext(Dispatchers.IO) {
        require(amountCents > 0L) { "El pago debe ser mayor a cero" }
        database.withTransaction {
            val supplier = supplierDao.getById(supplierId)
                ?: error("Proveedor no encontrado")
            val txId = applyTransactionLocked(
                accountId = accountId,
                amountCents = amountCents,
                type = TransactionType.EXPENSE,
                category = DefaultCategories.SUPPLIER_PAYMENT,
                date = DateTimeUtils.nowEpochMillis(),
                note = "Pago · ${supplier.name}",
                relatedSupplierId = supplierId
            )
            supplierDao.update(
                supplier.copy(totalPaid = Money.add(supplier.totalPaid, amountCents))
            )
            txId
        }.also { notifyWidgets() }
    }

    /**
     * Transferencia atómica entre cuentas.
     * Dos movimientos TRANSFER con el mismo [transferGroupId].
     */
    suspend fun transferBetweenAccounts(
        fromAccountId: Long,
        toAccountId: Long,
        amountCents: Long,
        note: String = ""
    ) = withContext(Dispatchers.IO) {
        require(fromAccountId != toAccountId) {
            "La cuenta de origen y destino deben ser diferentes"
        }
        require(amountCents > 0L) { "El monto debe ser mayor a cero" }
        database.withTransaction {
            val from = accountDao.getById(fromAccountId)
                ?: error("Cuenta de origen no encontrada")
            val to = accountDao.getById(toAccountId)
                ?: error("Cuenta de destino no encontrada")
            if (from.balance < amountCents) {
                error("Saldo insuficiente en «${from.name}» (${Money.format(from.balance)})")
            }

            val now = DateTimeUtils.nowEpochMillis()
            val groupId = now xor Random.nextLong()
            val extra = note.trim().takeIf { it.isNotEmpty() }?.let { " · $it" }.orEmpty()
            val fromNote = ("Transferencia a ${to.name}$extra").take(500)
            val toNote = ("Transferencia desde ${from.name}$extra").take(500)

            ledgerDao.postTransactionAndUpdateBalance(
                transaction = TransactionEntity(
                    accountId = fromAccountId,
                    amount = amountCents,
                    type = TransactionType.TRANSFER,
                    category = "Transferencia",
                    date = now,
                    note = fromNote,
                    transferGroupId = groupId,
                    transferIsOutbound = true
                ),
                newBalance = Money.subtract(from.balance, amountCents)
            )
            ledgerDao.postTransactionAndUpdateBalance(
                transaction = TransactionEntity(
                    accountId = toAccountId,
                    amount = amountCents,
                    type = TransactionType.TRANSFER,
                    category = "Transferencia",
                    date = now,
                    note = toNote,
                    transferGroupId = groupId,
                    transferIsOutbound = false
                ),
                newBalance = Money.add(to.balance, amountCents)
            )
        }
        notifyWidgets()
    }

    /**
     * Elimina un movimiento y revierte saldos (y CxC / proveedores si aplica).
     * TRANSFER: elimina ambas piernas del [transferGroupId].
     */
    suspend fun deleteTransaction(id: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val tx = transactionDao.getById(id)
                ?: error("Movimiento no encontrado")

            if (tx.type == TransactionType.TRANSFER) {
                deleteTransferPairLocked(tx)
                return@withTransaction
            }

            val account = accountDao.getById(tx.accountId)
                ?: error("Cuenta no encontrada")
            val newBalance = when (tx.type) {
                TransactionType.INCOME -> {
                    if (account.balance < tx.amount) {
                        error(
                            "Saldo insuficiente en «${account.name}» " +
                                "(${Money.format(account.balance)}) para revertir el ingreso"
                        )
                    }
                    Money.subtract(account.balance, tx.amount)
                }
                TransactionType.EXPENSE -> Money.add(account.balance, tx.amount)
                else -> error("Tipo inválido")
            }
            accountDao.update(account.copy(balance = newBalance))

            tx.relatedClientId?.let { clientId ->
                val client = clientDao.getById(clientId)
                if (client != null) {
                    clientDao.update(
                        client.copy(totalDebt = Money.add(client.totalDebt, tx.amount))
                    )
                }
            }
            tx.relatedSupplierId?.let { supplierId ->
                val supplier = supplierDao.getById(supplierId)
                if (supplier != null) {
                    if (supplier.totalPaid < tx.amount) {
                        error("No se puede revertir el pago: acumulado inconsistente")
                    }
                    supplierDao.update(
                        supplier.copy(totalPaid = Money.subtract(supplier.totalPaid, tx.amount))
                    )
                }
            }

            transactionDao.delete(tx)
        }
        notifyWidgets()
    }

    private suspend fun deleteTransferPairLocked(tx: TransactionEntity) {
        val groupId = tx.transferGroupId
        val legs = if (groupId != null) {
            val paired = transactionDao.getByTransferGroup(groupId)
            if (paired.isEmpty()) listOf(tx) else paired
        } else {
            listOf(tx)
        }

        legs.forEach { leg ->
            val account = accountDao.getById(leg.accountId)
                ?: error("Cuenta no encontrada")
            val isOutbound = when {
                leg.transferIsOutbound != null -> leg.transferIsOutbound == true
                leg.note.startsWith("Transferencia a") -> true
                leg.note.startsWith("Transferencia desde") -> false
                else -> error("Tipo de transferencia inválido")
            }
            val newBalance = if (isOutbound) {
                Money.add(account.balance, leg.amount)
            } else {
                if (account.balance < leg.amount) {
                    error(
                        "Saldo insuficiente en «${account.name}» " +
                            "(${Money.format(account.balance)})"
                    )
                }
                Money.subtract(account.balance, leg.amount)
            }
            accountDao.update(account.copy(balance = newBalance))
            transactionDao.delete(leg)
        }
    }

    private suspend fun createAccountInternal(
        name: String,
        type: String,
        initialBalanceCents: Long
    ): Long {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "El nombre de la cuenta es obligatorio" }
        require(trimmed.length <= 80) { "El nombre de la cuenta es demasiado largo" }
        require(type in AccountType.all) { "Tipo de cuenta inválido" }
        require(initialBalanceCents >= 0L) { "El saldo inicial no puede ser negativo" }
        return accountDao.insert(
            AccountEntity(
                name = trimmed,
                balance = initialBalanceCents,
                type = type
            )
        )
    }

    private suspend fun applyTransactionLocked(
        accountId: Long,
        amountCents: Long,
        type: String,
        category: String,
        date: Long,
        note: String,
        relatedClientId: Long? = null,
        relatedSupplierId: Long? = null
    ): Long {
        require(amountCents > 0L) { "El monto debe ser mayor a cero" }
        require(type == TransactionType.INCOME || type == TransactionType.EXPENSE) {
            "Tipo inválido"
        }
        val trimmedCategory = category.trim()
        require(trimmedCategory.isNotBlank()) { "La categoría es obligatoria" }

        val account = accountDao.getById(accountId)
            ?: error("Cuenta no encontrada")

        val newBalance = when (type) {
            TransactionType.INCOME -> Money.add(account.balance, amountCents)
            else -> {
                if (account.balance < amountCents) {
                    error("Saldo insuficiente en «${account.name}» (${Money.format(account.balance)})")
                }
                Money.subtract(account.balance, amountCents)
            }
        }

        return ledgerDao.postTransactionAndUpdateBalance(
            transaction = TransactionEntity(
                accountId = accountId,
                amount = amountCents,
                type = type,
                category = trimmedCategory,
                date = date,
                note = note.trim().take(500),
                relatedClientId = relatedClientId,
                relatedSupplierId = relatedSupplierId
            ),
            newBalance = newBalance
        )
    }

    companion object {
        private const val TAG = "FinanceRepo"
    }
}
